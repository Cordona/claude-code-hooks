package com.cordona.claudecodehooks.shared.models

import com.cordona.claudecodehooks.shared.enums.HookType

data class HookMetadata(
	val timestamp: String,
	val userExternalId: String,
	val claudeSessionId: String,
	val hostEventId: String,
	val hookType: HookType,
	val transcriptPath: String,
	val contextWorkDirectory: String
)