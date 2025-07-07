package com.cordona.claudecodehooks.infrastructure.messaging.internal.sse.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.messaging.server-sent-events")
data class SseProperties(
	val timeoutMillis: Long,
	val maxConnections: Int
)