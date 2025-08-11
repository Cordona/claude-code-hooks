package com.cordona.claudecodehooks.domain.internal.events.notification

import com.cordona.claudecodehooks.domain.external.api.NotificationEventProcessor
import com.cordona.claudecodehooks.infrastructure.external.api.EventPublisher
import com.cordona.claudecodehooks.shared.models.HookEvent
import com.cordona.claudecodehooks.shared.models.NotificationHook
import org.springframework.stereotype.Service

@Service
class NotificationEventService(
	private val eventPublisher: EventPublisher,
) : NotificationEventProcessor {

	override fun execute(target: NotificationHook) {
		val hookEvent = HookEvent(
			reason = resolveMessage(target.message),
			hookMetadata = target.hookMetadata,
			hostTelemetry = target.hostTelemetry
		)

		eventPublisher.publish(hookEvent)
	}

	private fun resolveMessage(message: String): String =
		if (isPlanApprovalRequest(message)) "Claude Code plan is ready for review" else message

	private fun isPlanApprovalRequest(message: String): Boolean {
		return message.matches(Regex("^Claude needs your permission to use\\s*$"))
	}
}