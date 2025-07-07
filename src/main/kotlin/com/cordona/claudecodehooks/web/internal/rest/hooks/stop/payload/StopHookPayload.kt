package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.payload

data class StopHookPayload(
	val stopHookActive: Boolean,
	val sessionId: String,
	val transcriptPath: String,
	val hookEventName: String,
)