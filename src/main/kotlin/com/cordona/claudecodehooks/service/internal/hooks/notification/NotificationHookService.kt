package com.cordona.claudecodehooks.service.internal.hooks.notification

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.EventPublisher
import com.cordona.claudecodehooks.service.external.api.NotificationHookProcessor
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import com.cordona.claudecodehooks.shared.models.NotificationHook
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class NotificationHookService(
	private val eventPublisher: EventPublisher
) : NotificationHookProcessor {

	override fun execute(target: NotificationHook) {
		val claudeHookEvent = ClaudeHookEvent(
			id = UUID.randomUUID().toString(),
			reason = resolveMessage(target.message),
			timestamp = Instant.now().toString(),
			contextWorkDirectory = target.contextWorkDirectory
		)

		eventPublisher.publish(claudeHookEvent)
	}

	private fun resolveMessage(message: String): String =
		if (isPlanApprovalRequest(message)) "Claude Code plan is ready for review" else message

	private fun isPlanApprovalRequest(message: String): Boolean {
		return message.matches(Regex("^Claude needs your permission to use\\s*$"))
	}
}