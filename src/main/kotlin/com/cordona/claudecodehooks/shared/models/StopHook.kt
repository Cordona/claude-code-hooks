package com.cordona.claudecodehooks.shared.models

data class StopHook(
    val stopHookActive: Boolean,
    override val metadata: HookMetadata
) : ClaudeHook()