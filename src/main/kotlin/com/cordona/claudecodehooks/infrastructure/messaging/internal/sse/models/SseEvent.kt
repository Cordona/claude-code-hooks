package com.cordona.claudecodehooks.infrastructure.messaging.internal.sse.models

import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent

data class SseEvent(
	val id: String,
	val reason: String,
	val timestamp: String,
	val contextWorkDirectory: String?,
) {

	companion object {
		fun from(source: ClaudeHookEvent): SseEvent = SseEvent(
			id = source.id,
			reason = source.reason,
			timestamp = source.timestamp,
			contextWorkDirectory = source.contextWorkDirectory
		)
	}
}