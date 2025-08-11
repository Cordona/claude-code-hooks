package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.event

import com.cordona.claudecodehooks.infrastructure.external.api.EventPublisher
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.shared.models.HookEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
class SseEventPublisher(
	private val connectionStore: ConnectionStore,
) : EventPublisher {

	private val logger = KotlinLogging.logger {}

	override fun publish(event: HookEvent) {
		val userExternalId = event.hookMetadata.userExternalId
		val userConnections = connectionStore.getUserConnections(userExternalId)

		if (userConnections.isNullOrEmpty()) {
			logger.debug { "No active connections found for user with external ID $userExternalId" }
			return
		}

		userConnections.forEach { metadata ->
			connectionStore.getEmitter(metadata.connectionId)?.let { emitter ->
				val success = sendEventToConnection(emitter, event)
				if (!success) {
					logger.warn { "Failed to deliver event to connection with ID ${metadata.connectionId}" }
				}
			}
		}

		logger.debug { "Event published to user with external ID $userExternalId (${userConnections.size} active connections)" }
	}

	private fun sendEventToConnection(emitter: SseEmitter, data: Any): Boolean {
		return runCatching {
			emitter.send(SseEmitter.event().name(CLAUDE_HOOK_EVENT).data(data))
		}.isSuccess
	}

	companion object {
		private const val CLAUDE_HOOK_EVENT = "claude-hook"
	}
}