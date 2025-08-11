package com.cordona.claudecodehooks.infrastructure.external.api

import com.cordona.claudecodehooks.shared.models.HookEvent

fun interface EventPublisher {
	fun publish(event: HookEvent)
}