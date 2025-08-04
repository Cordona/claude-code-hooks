package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.event

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.HookMetadata
import com.cordona.claudecodehooks.shared.models.StopHook
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class StopHookRequest(
    @JsonProperty("session_id") val sessionId: String,
    @JsonProperty("transcript_path") val transcriptPath: String,
    @JsonProperty("hook_event_name") val hookEventName: String,
    @JsonProperty("stop_hook_active") val stopHookActive: Boolean,
    val cwd: String
)

fun StopHookRequest.toModel(userExternalId: String): StopHook {
    return StopHook(
        stopHookActive = this.stopHookActive,
        contextWorkDirectory = this.cwd,
        metadata = HookMetadata(
            timestamp = Instant.now().toString(),
            hookType = HookType.fromValue(this.hookEventName),
            sessionId = this.sessionId,
            transcriptPath = this.transcriptPath,
            userExternalId = userExternalId
        )
    )
}