package com.cordona.claudecodehooks.infrastructure.external.api

import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent

fun interface EventPublisher {
	fun publish(userExternalId: String, event: ClaudeHookEvent)
}