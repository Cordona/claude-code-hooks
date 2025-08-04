package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.heartbeat

data class HeartbeatResult(
	val connectionId: String,
	val userExternalId: String,
	val success: Boolean,
	val error: String? = null,
	val deliveryTimeMs: Long
) {
	
	companion object {
		fun success(connectionId: String, userExternalId: String, deliveryTimeMs: Long): HeartbeatResult =
			HeartbeatResult(
				connectionId = connectionId,
				userExternalId = userExternalId,
				success = true,
				error = null,
				deliveryTimeMs = deliveryTimeMs
			)
		
		fun failure(connectionId: String, userExternalId: String, error: String, deliveryTimeMs: Long): HeartbeatResult =
			HeartbeatResult(
				connectionId = connectionId,
				userExternalId = userExternalId,
				success = false,
				error = error,
				deliveryTimeMs = deliveryTimeMs
			)
	}
}