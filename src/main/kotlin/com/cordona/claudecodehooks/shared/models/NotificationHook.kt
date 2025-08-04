package com.cordona.claudecodehooks.shared.models

data class NotificationHook(
	val message: String,
	val contextWorkDirectory: String?,
	override val metadata: HookMetadata,
) : ClaudeHook()