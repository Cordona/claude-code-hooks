package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.event

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.HookMetadata
import com.cordona.claudecodehooks.shared.models.StopHook
import com.cordona.claudecodehooks.shared.models.HostTelemetry
import com.fasterxml.jackson.annotation.JsonProperty

data class StopHookRequest(
	val payload: StopHookPayload,
	val timestamp: String,
	@field:JsonProperty("host_event_id") val hostEventId: String,
	@field:JsonProperty("host_telemetry") val hostTelemetry: HostTelemetry,
) {
	data class StopHookPayload(
		@field:JsonProperty("session_id") val sessionId: String,
		@field:JsonProperty("transcript_path") val transcriptPath: String,
		@field:JsonProperty("hook_event_name") val hookEventName: String,
		@field:JsonProperty("stop_hook_active") val stopHookActive: Boolean,
		val cwd: String,
	)

	fun toModel(userExternalId: String): StopHook {
		return StopHook(
			stopHookActive = this.payload.stopHookActive,
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