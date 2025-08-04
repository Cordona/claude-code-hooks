package com.cordona.claudecodehooks.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
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
	private val authHeaders: Map<String, String> = emptyMap(),
	private val sseAssertions: SseAssertions? = null
) : Closeable {
	private val logger = KotlinLogging.logger {}
	private val events = ConcurrentLinkedQueue<String>()
	private val connectionEvents = ConcurrentLinkedQueue<String>()
	private val heartbeats = ConcurrentLinkedQueue<String>()
	private val isRunning = AtomicBoolean(false)
	private val isConnected = AtomicBoolean(false)
	private var clientThread: Thread? = null

	companion object {
		const val LOCALHOST_BASE_URL = "http://localhost"
		
		fun create(
			serverPort: Int,
			sseEndpoint: String,
			restClientBuilder: RestClient.Builder,
			sseAssertions: SseAssertions,
			authHeaders: Map<String, String> = emptyMap()
		): SseTestClient {
			val baseUrl = "$LOCALHOST_BASE_URL:$serverPort"
			val restClient = restClientBuilder.baseUrl(baseUrl).build()
			
			return SseTestClient(
				restClient = restClient,
				sseEndpoint = sseEndpoint,
				authHeaders = authHeaders,
				sseAssertions = sseAssertions
			)
		}
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

	fun setupAndStart() {
		start()
		sseAssertions?.assertConnectionEstablished(this)
	}

	fun stopSafely() {
		if (isRunning.get()) {
			stop()
		}
	}

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

	fun waitForConnectionEvents(timeout: Duration, expectedCount: Int = 1): List<String> {
		val startTime = System.currentTimeMillis()

		while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
			if (connectionEvents.size >= expectedCount) {
				return connectionEvents.take(expectedCount)
			}
			Thread.sleep(50)
		}

		logger.warn { "Timeout waiting for $expectedCount connection events. Got ${connectionEvents.size} events in $timeout" }
		return connectionEvents.take(expectedCount)
	}

	fun waitForHeartbeats(timeout: Duration, expectedCount: Int = 1): List<String> {
		val startTime = System.currentTimeMillis()

		while (System.currentTimeMillis() - startTime < timeout.toMillis()) {
			if (heartbeats.size >= expectedCount) {
				return heartbeats.take(expectedCount)
			}
			Thread.sleep(50)
		}

		logger.warn { "Timeout waiting for $expectedCount heartbeats. Got ${heartbeats.size} heartbeats in $timeout" }
		return heartbeats.take(expectedCount)
	}

	private fun connectAndListen() {
		runCatching {
			val requestSpec = restClient
				.get()
				.uri(sseEndpoint)
				.header(ACCEPT, TEXT_EVENT_STREAM_VALUE)
				.header(CACHE_CONTROL, "no-cache")

			authHeaders.forEach { (name, value) ->
				requestSpec.header(name, value)
			}

			requestSpec.exchange { _, response ->
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
				line.startsWith(":") -> {
					val comment = line.substringAfter(":").trim()
					if (comment == "heartbeat") {
						heartbeats.offer(comment)
						logger.debug { "Captured heartbeat comment" }
					} else {
						logger.debug { "Ignoring comment: $comment" }
					}
				}

				line.startsWith("event:") -> {
					eventName = line.substringAfter("event:").trim()
				}

				line.startsWith("data:") -> {
					eventData = line.substringAfter("data:").trim()
				}

				line.isEmpty() && eventName != null && eventData != null -> {
					when (eventName) {
						"claude-hook" -> {
							events.offer(eventData)
							logger.info { "Captured claude-hook event data: $eventData" }
						}
						"connected" -> {
							connectionEvents.offer(eventData)
							logger.info { "Captured connection event data: $eventData" }
						}
						else -> {
							logger.debug { "Ignoring event: $eventName" }
						}
					}

					eventName = null
					eventData = null
				}
			}
		}
	}
}