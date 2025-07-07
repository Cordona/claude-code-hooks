package com.cordona.claudecodehooks.infrastructure.messaging.internal.sse

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.ConnectionManager
import com.cordona.claudecodehooks.infrastructure.messaging.internal.sse.properties.SseProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class SseConnectionManager(
	private val sseProperties: SseProperties,
) : ConnectionManager {

	private val logger = KotlinLogging.logger {}
	private val activeEmitters = ConcurrentHashMap<String, SseEmitter>()

	override fun connect(): SseEmitter {
		validateConnectionLimit()

		val emitter = SseEmitter(sseProperties.timeoutMillis)
		val emitterId = UUID.randomUUID().toString()

		setupConnectionLifecycleHandlers(emitter, emitterId)
		
		activeEmitters[emitterId] = emitter
		logger.info { "SSE connection established. Active connections: ${activeEmitters.size}" }
		
		sendConnectionConfirmation(emitter, emitterId)

		return emitter
	}

	fun broadcast(eventName: String, data: Any) {
		if (activeEmitters.isEmpty()) return

		val emittersSnapshot = activeEmitters.toMap()
		val failedConnectionIds = broadcastAndCollectFailures(emittersSnapshot, eventName, data)
		
		failedConnectionIds.forEach { activeEmitters.remove(it) }
		
		if (failedConnectionIds.isNotEmpty()) {
			logger.info { "SSE broadcast: ${activeEmitters.size} successful, ${failedConnectionIds.size} dead connections cleaned up" }
		}
	}

	fun getActiveConnectionCount(): Int = activeEmitters.size

	private fun validateConnectionLimit() {
		if (activeEmitters.size >= sseProperties.maxConnections) {
			logger.warn { "Maximum SSE connections reached (${sseProperties.maxConnections}). Rejecting new connection." }
			throw IllegalStateException("Maximum SSE connections reached")
		}
	}

	private fun setupConnectionLifecycleHandlers(emitter: SseEmitter, emitterId: String) {
		emitter.onCompletion {
			activeEmitters.remove(emitterId)
		}
		emitter.onTimeout {
			activeEmitters.remove(emitterId)
		}
		emitter.onError {
			activeEmitters.remove(emitterId)
		}
	}

	private fun sendConnectionConfirmation(emitter: SseEmitter, emitterId: String) {
		runCatching {
			val connectionData = mapOf(
				"emitterId" to emitterId,
				"message" to "Connected to Claude Code Hooks"
			)
			emitter.send(SseEmitter.event().name(CONNECTED_EVENT).data(connectionData))
		}.onFailure {
			// Connection might still be valid for future events
		}
	}

	private fun broadcastAndCollectFailures(
		emitters: Map<String, SseEmitter>,
		eventName: String,
		data: Any,
	): List<String> {
		val failedConnectionIds = mutableListOf<String>()

		emitters.forEach { (id, emitter) ->
			val isConnectionActive = activeEmitters.containsKey(id)
			val broadcastFailed = sendEventToConnection(emitter, eventName, data)
			
			if (isConnectionActive && broadcastFailed) {
				failedConnectionIds.add(id)
			}
		}

		return failedConnectionIds
	}

	private fun sendEventToConnection(emitter: SseEmitter, eventName: String, data: Any): Boolean {
		return runCatching { emitter.send(SseEmitter.event().name(eventName).data(data)) }.isFailure
	}
	
	companion object {
		private const val CONNECTED_EVENT = "connected"
	}
}