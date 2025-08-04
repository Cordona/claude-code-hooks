package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection

data class ConnectionConfirmation(
	val connectionId: String,
	val message: String,
)