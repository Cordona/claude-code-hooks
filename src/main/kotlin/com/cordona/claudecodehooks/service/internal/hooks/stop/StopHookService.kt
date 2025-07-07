package com.cordona.claudecodehooks.service.internal.hooks.stop

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.EventPublisher
import com.cordona.claudecodehooks.service.external.api.StopHookProcessor
import com.cordona.claudecodehooks.service.internal.hooks.resolveProjectContext
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import com.cordona.claudecodehooks.shared.models.StopHook
import org.springframework.stereotype.Service

@Service
class StopHookService(
	private val eventPublisher: EventPublisher
) : StopHookProcessor {

	override fun execute(target: StopHook) {
		val claudeHookEvent = ClaudeHookEvent(
			timestamp = target.metadata.timestamp,
			message = STOP_HOOK_DEFAULT_MESSAGE,
			projectContext = target.metadata.transcriptPath.resolveProjectContext()
		)

		eventPublisher.publish(claudeHookEvent)
	}

	companion object {
		const val STOP_HOOK_DEFAULT_MESSAGE = "Continue your claude session"
	}

}