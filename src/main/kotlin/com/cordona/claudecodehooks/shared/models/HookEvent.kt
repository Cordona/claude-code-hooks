package com.cordona.claudecodehooks.shared.models

data class HookEvent(
	val reason: String,
	val hookMetadata: HookMetadata,
	val hostTelemetry: HostTelemetry,
)