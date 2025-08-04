package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.heartbeat

data class HeartbeatCycleResult(
	var successCount: Int = 0,
	var failureCount: Int = 0,
	val staleConnections: MutableList<String> = mutableListOf(),
) {
	fun recordSuccess() {
		successCount++
	}

	fun recordFailure() {
		failureCount++
	}

	fun addStaleConnection(connectionId: String) {
		staleConnections.add(connectionId)
	}

	fun hasStaleConnections() = staleConnections.isNotEmpty()

	fun staleConnectionCount() = staleConnections.size
}