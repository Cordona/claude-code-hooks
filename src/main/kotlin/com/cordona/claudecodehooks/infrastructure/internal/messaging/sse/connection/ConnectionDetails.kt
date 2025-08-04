package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection

import java.time.Instant

data class ConnectionDetails(
	val connectionId: String,
	val userExternalId: String,
	val health: ConnectionHealth = ConnectionHealth(),
) {
	fun recordSuccess(): ConnectionDetails = copy(health = health.resetFailures())
	
	fun recordFailure(): ConnectionDetails = copy(health = health.incrementFailures())

	fun isUnhealthy(maxFailures: Int): Boolean = health.isUnhealthy(maxFailures)

	data class ConnectionHealth(
		private var consecutiveFailures: Int = 0,
		private var lastFailureTime: Instant? = null,
	) {
		fun incrementFailures(): ConnectionHealth {
			consecutiveFailures++
			lastFailureTime = Instant.now()
			return this
		}
		
		fun resetFailures(): ConnectionHealth {
			consecutiveFailures = 0
			lastFailureTime = null
			return this
		}
		
		fun isUnhealthy(maxFailures: Int): Boolean = consecutiveFailures >= maxFailures
		
		fun getConsecutiveFailures(): Int = consecutiveFailures
	}
}