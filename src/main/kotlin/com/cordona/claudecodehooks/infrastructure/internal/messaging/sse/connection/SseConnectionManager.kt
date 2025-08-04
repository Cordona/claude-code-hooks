package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection

import com.cordona.claudecodehooks.infrastructure.external.api.ConnectionManager
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties.SseProperties
import com.cordona.claudecodehooks.shared.enums.DisconnectionResult
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@Component
class SseConnectionManager(
	private val sseProperties: SseProperties,
	private val connectionStore: ConnectionStore,
	private val objectMapper: ObjectMapper,
) : ConnectionManager {

	private val logger = KotlinLogging.logger {}

	override fun connect(userExternalId: String): SseEmitter {
		val emitter = SseEmitter(sseProperties.timeoutMillis)
		val connectionId = UUID.randomUUID().toString()
		val connectionDetails = ConnectionDetails(
			connectionId = connectionId,
			userExternalId = userExternalId
		)

		connectionStore.addConnection(userExternalId, connectionDetails, emitter)

		logger.debug { "SSE connection established. Total active connections: ${connectionStore.getConnectionsCount()}" }

		sendConnectionConfirmation(emitter, connectionId)

		return emitter
	}

	override fun disconnect(connectionId: String, userExternalId: String): DisconnectionResult {
		val emitter = connectionStore.getEmitter(connectionId)

		if (emitter == null) {
			logger.debug { "Connection with ID $connectionId not found for disconnection" }
			return DisconnectionResult.NOT_FOUND
		}

		val removed = connectionStore.removeUserConnection(userExternalId, connectionId)

		return if (removed) {
			logger.debug { "Disconnected connection with ID $connectionId for user with external ID $userExternalId" }
			DisconnectionResult.SUCCESS
		} else {
			logger.warn { "User with external ID $userExternalId attempted to disconnect connection with ID $connectionId that doesn't belong to them" }
			DisconnectionResult.FORBIDDEN
		}
	}

	fun cleanupStaleConnections(connectionIds: List<String>): Int {
		var removedCount = 0

		connectionIds.forEach { connectionId ->
			logger.debug { "Cleaning up stale connection with ID $connectionId" }

			runCatching {
				val removed = cleanupStaleConnection(connectionId)
				if (removed) {
					removedCount++
					logger.debug { "Successfully removed stale connection with ID $connectionId" }
				} else {
					logger.debug { "Stale connection with ID $connectionId was not found in cache (race condition)" }
				}
			}.onFailure { exception ->
				logger.warn(exception) { "Error during stale connection cleanup for connection with ID $connectionId" }
			}
		}

		return removedCount
	}

	fun cleanupStaleConnection(connectionId: String): Boolean {
		val emitter = connectionStore.getEmitter(connectionId)
		if (emitter != null) {
			runCatching { emitter.complete() }
		}

		return connectionStore.removeConnection(connectionId)
	}

	private fun sendConnectionConfirmation(emitter: SseEmitter, connectionId: String) {
		runCatching {
			val connectionConfirmation = ConnectionConfirmation(
				connectionId = connectionId,
				message = "Connected to Claude Code Hooks"
			)
			val jsonData = objectMapper.writeValueAsString(connectionConfirmation)
			emitter.send(SseEmitter.event().name(CONNECTED_EVENT).data(jsonData))
		}.onFailure { exception ->
			logger.warn(exception) { "Failed to send connection confirmation for connection with ID $connectionId" }
		}
	}

	companion object {
		private const val CONNECTED_EVENT = "connected"
	}
}