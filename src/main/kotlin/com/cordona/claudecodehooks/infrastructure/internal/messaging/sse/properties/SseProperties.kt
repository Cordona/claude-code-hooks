package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.messaging.server-sent-events")
data class SseProperties(
	val timeoutMillis: Long,
	val maxConnections: Int,
	val cache: CacheProperties,
	val concurrency: ConcurrencyProperties,
	val heartbeat: HeartbeatProperties,
) {
	data class CacheProperties(
		val maxUsers: Long,
		val maxConnections: Long,
	)

	data class ConcurrencyProperties(
		val virtualThreadsLimit: Int,
	)

	data class HeartbeatProperties(
		val intervalSeconds: Long,
		val jitterEnabled: Boolean,
		val timeoutSeconds: Long,
		val eventType: HeartbeatEventType,
		val maxFailures: Int,
	) {
		enum class HeartbeatEventType {
			COMMENT
		}

		fun getIntervalMillis(): Long = intervalSeconds * MILLIS_PER_SECOND
		fun getTimeoutMillis(): Long = timeoutSeconds * MILLIS_PER_SECOND
		fun getJitterMaxDelayMillis(): Long = if (jitterEnabled) getIntervalMillis() else NO_JITTER_DELAY

		companion object {
			private const val MILLIS_PER_SECOND = 1000L
			private const val NO_JITTER_DELAY = 0L
		}
	}
}