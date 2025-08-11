package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.event

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.HookMetadata
import com.cordona.claudecodehooks.shared.models.NotificationHook
import com.cordona.claudecodehooks.shared.models.HostTelemetry
import com.fasterxml.jackson.annotation.JsonProperty

data class NotificationHookRequest(
	@field:JsonProperty("host_event_id")
	val hostEventId: String,
	val payload: NotificationHookPayload,
	@field:JsonProperty("host_telemetry")
	val hostTelemetry: HostTelemetry,
	val timestamp: String
) {
	data class NotificationHookPayload(
		@field:JsonProperty("session_id")
		val sessionId: String,
		@field:JsonProperty("transcript_path")
		val transcriptPath: String,
		@field:JsonProperty("hook_event_name")
		val hookEventName: String,
		val message: String,
		val cwd: String,
	)


	fun toModel(userExternalId: String): NotificationHook {
		return NotificationHook(
			message = this.payload.message,
			hookMetadata = HookMetadata(
				timestamp = this.timestamp,
				userExternalId = userExternalId,
				claudeSessionId = this.payload.sessionId,
				hostEventId = this.hostEventId,
				hookType = HookType.fromValue(this.payload.hookEventName),
				transcriptPath = this.payload.transcriptPath,
				contextWorkDirectory = this.payload.cwd
			),
			hostTelemetry = this.hostTelemetry
		)
	}
}