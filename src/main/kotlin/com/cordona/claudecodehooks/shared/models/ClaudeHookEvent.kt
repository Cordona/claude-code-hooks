package com.cordona.claudecodehooks.shared.models

import com.cordona.claudecodehooks.shared.enums.HookType

data class ClaudeHookEvent(
	val id: String,
	val hookType: HookType,
	val reason: String,
	val timestamp: String,
	val contextWorkDirectory: String?,
	val userExternalId: String,
)