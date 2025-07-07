package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.request

import com.cordona.claudecodehooks.shared.enums.HookType
import com.cordona.claudecodehooks.shared.models.HookMetadata
import com.cordona.claudecodehooks.shared.models.StopHook
import java.time.Instant

data class StopHookRequest(
    val sessionId: String,
    val transcriptPath: String,
    val hookEventName: String,
    val stopHookActive: Boolean
)

fun StopHookRequest.toModel(): StopHook {
    return StopHook(
        stopHookActive = this.stopHookActive,
        metadata = HookMetadata(
            timestamp = Instant.now().toString(),
            hookType = HookType.fromValue(this.hookEventName),
            sessionId = this.sessionId,
            transcriptPath = this.transcriptPath
        )
    )
}