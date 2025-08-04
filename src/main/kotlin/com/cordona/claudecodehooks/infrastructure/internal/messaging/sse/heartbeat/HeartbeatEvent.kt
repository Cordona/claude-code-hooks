package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.heartbeat

data class HeartbeatEvent(
	val connectionId: String,
	val userExternalId: String
) {
	
	companion object {
		fun create(connectionId: String, userExternalId: String): HeartbeatEvent =
			HeartbeatEvent(
				connectionId = connectionId,
				userExternalId = userExternalId
			)
	}
}