package com.cordona.claudecodehooks.security.internal.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.security")
data class SecurityProperties(
	val cors: CorsProperties,
) {
	data class CorsProperties(
		val maxAgeSeconds: Long,
		val allowCredentials: Boolean,
		val allowedHeaders: List<String>,
		val allowedMethods: List<String>,
		val allowedOrigins: List<String>,
		val enabledPaths: List<String>,
	)
}