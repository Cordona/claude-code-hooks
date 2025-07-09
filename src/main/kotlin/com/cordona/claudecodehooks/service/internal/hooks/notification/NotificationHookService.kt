package com.cordona.claudecodehooks.service.internal.hooks.notification

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.EventPublisher
import com.cordona.claudecodehooks.service.external.api.NotificationHookProcessor
import com.cordona.claudecodehooks.service.internal.hooks.resolveProjectContext
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import com.cordona.claudecodehooks.shared.models.NotificationHook
import org.springframework.stereotype.Service

@Service
class NotificationHookService(
	private val eventPublisher: EventPublisher
) : NotificationHookProcessor {

	override fun execute(target: NotificationHook) {
		val claudeHookEvent = ClaudeHookEvent(
			timestamp = target.metadata.timestamp,
			message = resolveMessage(target.message),
			projectContext = target.metadata.transcriptPath.resolveProjectContext()
		)

		eventPublisher.publish(claudeHookEvent)
	}

	private fun resolveMessage(message: String): String =
		if (isPlanApprovalRequest(message)) "Claude Code plan is ready for review" else message

	private fun isPlanApprovalRequest(message: String): Boolean {
		return message.matches(Regex("^Claude needs your permission to use\\s*$"))
	}
}