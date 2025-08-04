package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.event

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent

data class SseEvent(
	val id: String,
	val hookType: HookType,
	val reason: String,
	val timestamp: String,
	val contextWorkDirectory: String?,
	val userExternalId: String,
) {

	companion object {
		fun from(source: ClaudeHookEvent): SseEvent = SseEvent(
			id = source.id,
			hookType = source.hookType,
			reason = source.reason,
			timestamp = source.timestamp,
			contextWorkDirectory = source.contextWorkDirectory,
			userExternalId = source.userExternalId
		)
	}
}