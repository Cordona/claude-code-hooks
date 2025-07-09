package com.cordona.claudecodehooks.shared.models

data class ClaudeHookEvent(
	val id: String,
	val reason: String,
	val timestamp: String,
	val contextWorkDirectory: String?
)