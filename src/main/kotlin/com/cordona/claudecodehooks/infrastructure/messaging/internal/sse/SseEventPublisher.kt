package com.cordona.claudecodehooks.infrastructure.messaging.internal.sse

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.EventPublisher
import com.cordona.claudecodehooks.infrastructure.messaging.internal.sse.models.SseEvent
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class SseEventPublisher(
	private val connectionManager: SseConnectionManager
) : EventPublisher {
	
	private val logger = KotlinLogging.logger {}

	override fun publish(event: ClaudeHookEvent) {
		connectionManager.broadcast(CLAUDE_HOOK_EVENT, SseEvent.from(event))
		logger.debug { "Event broadcasted to ${connectionManager.getActiveConnectionCount()} active connections" }
	}
	
	companion object {
		private const val CLAUDE_HOOK_EVENT = "claude-hook"
	}
}