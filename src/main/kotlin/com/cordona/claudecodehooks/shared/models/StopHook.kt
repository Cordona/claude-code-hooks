package com.cordona.claudecodehooks.shared.models

data class StopHook(
    val stopHookActive: Boolean,
    override val hookMetadata: HookMetadata,
    override val hostTelemetry: HostTelemetry,
) : ClaudeHook()