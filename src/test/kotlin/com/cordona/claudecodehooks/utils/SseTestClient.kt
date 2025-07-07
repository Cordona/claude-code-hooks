package com.cordona.claudecodehooks.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.io.BufferedReader
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class SseTestClient(
	private val restClient: RestClient,
	private val sseEndpoint: String,
) : Closeable {
	private val logger = KotlinLogging.logger {}
	private val events = ConcurrentLinkedQueue<String>()
	private val isRunning = AtomicBoolean(false)
	private val isConnected = AtomicBoolean(false)
	private var clientThread: Thread? = null

	companion object {
		const val LOCALHOST_BASE_URL = "http://localhost"
	}

	fun start() {
		if (isRunning.compareAndSet(false, true)) {
			clientThread = thread(name = "sse-test-client") {
				connectAndListen()
			}
			logger.debug { "SSE test client started for endpoint: $sseEndpoint" }
		}
	}

	fun stop() {
		if (isRunning.compareAndSet(true, false)) {
			clientThread?.interrupt()
			clientThread?.join(5000)
			logger.debug { "SSE test client stopped. Collected ${events.size} events" }
		}
	}

	override fun close() = stop()

	fun waitForConnection(timeout: Duration = Duration.ofSeconds(5)): Boolean {
		val startTime = System.currentTimeMillis()
		while (!isConnected.get() && System.currentTimeMillis() - startTime < timeout.toMillis()) {
			Thread.sleep(50)
		}
		return isConnected.get()
	}

	fun waitForEvents(timeout: Duration, expectedCount: Int = 1): List<String> {
		val startTime = System.currentTimeMillis()

		while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
			if (events.size >= expectedCount) {
				return events.take(expectedCount)
			}
			Thread.sleep(50)
		}

		logger.warn { "Timeout waiting for $expectedCount events. Got ${events.size} events in $timeout" }
		return events.take(expectedCount)
	}

	private fun connectAndListen() {
		runCatching {
			restClient
				.get()
				.uri(sseEndpoint)
				.header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
				.header(HttpHeaders.CACHE_CONTROL, "no-cache")
				.exchange { _, response ->
					isConnected.set(true)
					logger.debug { "SSE connection established" }
					response.body.use { inputStream ->
						inputStream.bufferedReader().use { reader ->
							parseEventStreamRealTime(reader)
						}
					}
				}
		}.onFailure { e ->
			when (e) {
				is InterruptedException -> logger.debug { "SSE client interrupted" }
				else -> logger.error(e) { "Error in SSE client: ${e.message}" }
			}
		}
	}

	private fun parseEventStreamRealTime(reader: BufferedReader) {
		var eventName: String? = null
		var eventData: String? = null

		while (isRunning.get()) {
			val line = reader.readLine() ?: break
			logger.debug { "SSE client received line: '$line'" }

			when {
				line.startsWith("event:") -> {
					eventName = line.substringAfter("event:").trim()
				}

				line.startsWith("data:") -> {
					eventData = line.substringAfter("data:").trim()
				}

				line.isEmpty() && eventName != null && eventData != null -> {
					if (eventName == "claude-hook") {
						events.offer(eventData)
						logger.info { "Captured claude-hook event data: $eventData" }
					} else {
						logger.debug { "Ignoring event: $eventName" }
					}

					eventName = null
					eventData = null
				}
			}
		}
	}
}