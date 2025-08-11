package com.cordona.claudecodehooks.shared.models

sealed class ClaudeHook {
	abstract val hookMetadata: HookMetadata
	abstract val hostTelemetry: HostTelemetry
}