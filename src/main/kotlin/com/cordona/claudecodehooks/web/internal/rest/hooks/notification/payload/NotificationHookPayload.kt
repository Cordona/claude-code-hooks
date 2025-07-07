package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.payload

data class NotificationHookPayload(
	val message: String,
	val sessionId: String,
	val transcriptPath: String,
	val hookEventName: String,
)