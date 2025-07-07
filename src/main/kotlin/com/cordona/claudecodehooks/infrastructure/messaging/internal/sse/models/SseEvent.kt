package com.cordona.claudecodehooks.infrastructure.messaging.internal.sse.models

import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import java.util.UUID

data class SseEvent(
	val id: String,
	val message: String,
	val timestamp: String,
	val projectContext: String,
) {

	companion object {
		fun from(source: ClaudeHookEvent): SseEvent = SseEvent(
			id = generateId(),
			message = source.message,
			timestamp = source.timestamp,
			projectContext = source.projectContext
		)

		private fun generateId(): String = UUID.randomUUID().toString()
	}
}