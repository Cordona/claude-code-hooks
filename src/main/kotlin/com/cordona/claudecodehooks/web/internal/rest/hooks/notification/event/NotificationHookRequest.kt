package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.event

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.HookMetadata
import com.cordona.claudecodehooks.shared.models.NotificationHook
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class NotificationHookRequest(
    @JsonProperty("session_id") val sessionId: String,
    @JsonProperty("transcript_path") val transcriptPath: String,
    @JsonProperty("hook_event_name") val hookEventName: String,
    val message: String,
    val cwd: String,
)

fun NotificationHookRequest.toModel(userExternalId: String): NotificationHook {
    return NotificationHook(
        message = this.message,
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