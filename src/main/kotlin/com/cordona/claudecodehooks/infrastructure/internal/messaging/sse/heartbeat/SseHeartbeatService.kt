package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.heartbeat

import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection.SseConnectionManager
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties.SseProperties
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties.SseProperties.HeartbeatProperties.HeartbeatEventType.COMMENT
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

		activeConnections.forEach { (connection, emitter) ->
			val jitterDelay = calculateJitterDelay()

			if (jitterDelay > ZERO_COUNT) {
				Thread.sleep(jitterDelay)
			}

			val heartbeatEvent = HeartbeatEvent.Companion.create(
				connectionId = connection.connectionId,
				userExternalId = connection.userExternalId
			)

			val heartbeatResult = sendHeartbeatToConnection(heartbeatEvent, emitter)

			if (heartbeatResult.success) {
				result.recordSuccess()
				val updatedConnection = connection.recordSuccess()
				connectionStore.updateConnectionHealth(connection.connectionId, updatedConnection)
				logger.trace { "Heartbeat delivered to connection with ID ${connection.connectionId} in ${heartbeatResult.deliveryTimeMs}ms" }
			} else {
				result.recordFailure()
				val updatedConnection = connection.recordFailure()
				connectionStore.updateConnectionHealth(connection.connectionId, updatedConnection)
				logger.warn { "Heartbeat failed for connection with ID ${connection.connectionId}: ${heartbeatResult.error}" }

				if (updatedConnection.isUnhealthy(sseProperties.heartbeat.maxFailures)) {
					result.addStaleConnection(connection.connectionId)
					logger.info { "Connection with ID ${connection.connectionId} marked as stale after ${updatedConnection.health.getConsecutiveFailures()} consecutive failures" }
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
				COMMENT -> {
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

	companion object {
		private const val HEARTBEAT_COMMENT = "heartbeat"
		private const val NO_JITTER_DELAY = 0L
		private const val ZERO_COUNT = 0
	}
}