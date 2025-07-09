package com.cordona.claudecodehooks.service.internal.hooks.stop

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.EventPublisher
import com.cordona.claudecodehooks.service.external.api.StopHookProcessor
import com.cordona.claudecodehooks.shared.models.ClaudeHookEvent
import com.cordona.claudecodehooks.shared.models.StopHook
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class StopHookService(
	private val eventPublisher: EventPublisher
) : StopHookProcessor {

	override fun execute(target: StopHook) {
		val claudeHookEvent = ClaudeHookEvent(
			id = UUID.randomUUID().toString(),
			reason = STOP_HOOK_DEFAULT_MESSAGE,
			timestamp = Instant.now().toString(),
			contextWorkDirectory = target.contextWorkDirectory
		)

		eventPublisher.publish(claudeHookEvent)
	}

	companion object {
		const val STOP_HOOK_DEFAULT_MESSAGE = "Continue your claude session"
	}

}