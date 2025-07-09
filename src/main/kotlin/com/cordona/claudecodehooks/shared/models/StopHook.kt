package com.cordona.claudecodehooks.shared.models

data class StopHook(
    val stopHookActive: Boolean,
    val contextWorkDirectory: String?,
    override val metadata: HookMetadata
) : ClaudeHook()