package com.cordona.claudecodehooks.shared.models

data class NotificationHook(
	val message: String,
	override val hookMetadata: HookMetadata,
	override val hostTelemetry: HostTelemetry,
) : ClaudeHook()