package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.heartbeat

import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection.SseConnectionManager
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties.SseProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.random.Random

@Service
class SseHeartbeatService(
	private val sseProperties: SseProperties,
	private val connectionStore: ConnectionStore,
	private val connectionManager: SseConnectionManager,
) {

	private val logger = KotlinLogging.logger {}

	@Scheduled(fixedRateString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(\${service.messaging.server-sent-events.heartbeat.interval-seconds})}")
	fun sendHeartbeats() {
		val activeConnections = connectionStore.getAllConnections()
		logger.debug { "Starting heartbeat delivery to ${activeConnections.size} active connections" }

		val result = HeartbeatCycleResult()

		activeConnections.forEach { (metadata, emitter) ->
			val jitterDelay = calculateJitterDelay()
			val heartbeatEvent = HeartbeatEvent.Companion.create(
				connectionId = metadata.connectionId,
				userExternalId = metadata.userExternalId
			)

			if (jitterDelay > ZERO_COUNT) {
				Thread.sleep(jitterDelay)
			}

			val heartbeatResult = sendHeartbeatToConnection(heartbeatEvent, emitter)

			if (heartbeatResult.success) {
				result.recordSuccess()
				logger.trace { "Heartbeat delivered to connection with ID ${metadata.connectionId} in ${heartbeatResult.deliveryTimeMs}ms" }
			} else {
				result.recordFailure()
				logger.warn { "Heartbeat failed for connection with ID ${metadata.connectionId}: ${heartbeatResult.error}" }

				if (isStaleConnectionError(heartbeatResult.error)) {
					result.addStaleConnection(metadata.connectionId)
				}
			}
		}

		connectionManager.cleanupStaleConnections(result.staleConnections)

		logger.debug {
			buildString {
				append("Heartbeat cycle completed: ")
				append("${result.successCount} successful, ${result.failureCount} failed")
				if (result.hasStaleConnections()) {
					append(", ${result.staleConnectionCount()} stale connections removed")
				}
			}
		}
	}

	private fun sendHeartbeatToConnection(heartbeatEvent: HeartbeatEvent, emitter: SseEmitter): HeartbeatResult {
		val startTime = System.currentTimeMillis()

		return try {
			when (sseProperties.heartbeat.eventType) {
				SseProperties.HeartbeatProperties.HeartbeatEventType.COMMENT -> {
					emitter.send(SseEmitter.event().comment(HEARTBEAT_COMMENT))
				}
			}

			val deliveryTime = System.currentTimeMillis() - startTime

			HeartbeatResult.Companion.success(
				connectionId = heartbeatEvent.connectionId,
				userExternalId = heartbeatEvent.userExternalId,
				deliveryTimeMs = deliveryTime
			)
		} catch (exception: Exception) {
			val deliveryTime = System.currentTimeMillis() - startTime

			HeartbeatResult.Companion.failure(
				connectionId = heartbeatEvent.connectionId,
				userExternalId = heartbeatEvent.userExternalId,
				error = "${exception.javaClass.simpleName}: ${exception.message}",
				deliveryTimeMs = deliveryTime
			)
		}
	}

	private fun calculateJitterDelay(): Long {
		return if (sseProperties.heartbeat.jitterEnabled) {
			Random.Default.nextLong(ZERO_COUNT.toLong(), sseProperties.heartbeat.getJitterMaxDelayMillis())
		} else {
			NO_JITTER_DELAY
		}
	}

	private fun isStaleConnectionError(errorMessage: String?): Boolean {
		return errorMessage?.let { message ->
			STALE_CONNECTION_ERROR_KEYWORDS.any { keyword -> message.contains(keyword) }
		} ?: false
	}

	companion object {
		private const val HEARTBEAT_COMMENT = "heartbeat"
		private const val NO_JITTER_DELAY = 0L
		private const val ZERO_COUNT = 0
		private val STALE_CONNECTION_ERROR_KEYWORDS = listOf(
			"IOException",
			"IllegalStateException",
			"broken"
		)
	}
}