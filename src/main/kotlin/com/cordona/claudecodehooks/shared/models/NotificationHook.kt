package com.cordona.claudecodehooks.shared.models

data class NotificationHook(
    val message: String,
    override val metadata: HookMetadata,
) : ClaudeHook()