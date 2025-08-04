package com.cordona.claudecodehooks.domain.internal.events.stop

import com.cordona.claudecodehooks.domain.external.api.StopEventProcessor
import com.cordona.claudecodehooks.infrastructure.external.api.EventPublisher
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import com.cordona.claudecodehooks.shared.models.StopHook
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class StopEventService(
	private val eventPublisher: EventPublisher,
) : StopEventProcessor {

	override fun execute(target: StopHook) {
		val claudeHookEvent = ClaudeHookEvent(
			id = UUID.randomUUID().toString(),
			hookType = target.metadata.hookType,
			reason = STOP_HOOK_DEFAULT_MESSAGE,
			timestamp = Instant.now().toString(),
			contextWorkDirectory = target.contextWorkDirectory,
			userExternalId = target.metadata.userExternalId
		)

		eventPublisher.publish(claudeHookEvent.userExternalId, claudeHookEvent)
	}

	companion object {
		const val STOP_HOOK_DEFAULT_MESSAGE = "Continue your claude session"
	}
}