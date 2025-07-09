package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.request

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.HookMetadata
import com.cordona.claudecodehooks.shared.models.NotificationHook
import java.time.Instant

data class NotificationHookRequest(
    val sessionId: String,
    val transcriptPath: String,
    val hookEventName: String,
    val message: String
)

fun NotificationHookRequest.toModel(contextWorkDirectory: String?): NotificationHook {
    return NotificationHook(
        message = this.message,
        contextWorkDirectory = contextWorkDirectory,
        metadata = HookMetadata(
            timestamp = Instant.now().toString(),
            hookType = HookType.fromValue(this.hookEventName),
            sessionId = this.sessionId,
            transcriptPath = this.transcriptPath
        )
    )
}