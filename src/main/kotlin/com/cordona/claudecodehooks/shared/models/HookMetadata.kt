package com.cordona.claudecodehooks.shared.models

import com.cordona.claudecodehooks.shared.enums.HookType

data class HookMetadata(
    val timestamp: String,
    val hookType: HookType,
    val sessionId: String,
    val transcriptPath: String,
)