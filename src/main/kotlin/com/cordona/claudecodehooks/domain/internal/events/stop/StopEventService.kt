package com.cordona.claudecodehooks.domain.internal.events.stop

import com.cordona.claudecodehooks.domain.external.api.StopEventProcessor
import com.cordona.claudecodehooks.infrastructure.external.api.EventPublisher
import com.cordona.claudecodehooks.shared.models.HookEvent
import com.cordona.claudecodehooks.shared.models.StopHook
import org.springframework.stereotype.Service

@Service
class StopEventService(
	private val eventPublisher: EventPublisher,
) : StopEventProcessor {

	override fun execute(target: StopHook) {
		val hookEvent = HookEvent(
			reason = STOP_HOOK_DEFAULT_MESSAGE,
			hookMetadata = target.hookMetadata,
			hostTelemetry = target.hostTelemetry
		)

		eventPublisher.publish(hookEvent)
	}

	companion object {
		const val STOP_HOOK_DEFAULT_MESSAGE = "Continue your claude session"
	}
}