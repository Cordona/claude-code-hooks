package com.cordona.claudecodehooks.infrastructure.messaging.external.api

import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent

fun interface EventPublisher {
	fun publish(event: ClaudeHookEvent)
}