package com.cordona.claudecodehooks.shared.models

data class ClaudeHookEvent(
	val timestamp: String,
	val message: String,
	val projectContext: String
)