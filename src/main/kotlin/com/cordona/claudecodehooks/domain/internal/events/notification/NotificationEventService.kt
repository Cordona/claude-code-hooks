package com.cordona.claudecodehooks.domain.internal.events.notification

import com.cordona.claudecodehooks.infrastructure.external.api.EventPublisher
import com.cordona.claudecodehooks.domain.external.api.NotificationEventProcessor
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import com.cordona.claudecodehooks.shared.models.NotificationHook
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class NotificationEventService(
	private val eventPublisher: EventPublisher
) : NotificationEventProcessor {

	override fun execute(target: NotificationHook) {
		val claudeHookEvent = ClaudeHookEvent(
			id = UUID.randomUUID().toString(),
			hookType = target.metadata.hookType,
			reason = resolveMessage(target.message),
			timestamp = Instant.now().toString(),
			contextWorkDirectory = target.contextWorkDirectory,
			userExternalId = target.metadata.userExternalId
		)

		eventPublisher.publish(claudeHookEvent.userExternalId, claudeHookEvent)
	}

	private fun resolveMessage(message: String): String =
		if (isPlanApprovalRequest(message)) "Claude Code plan is ready for review" else message

	private fun isPlanApprovalRequest(message: String): Boolean {
		return message.matches(Regex("^Claude needs your permission to use\\s*$"))
	}
}