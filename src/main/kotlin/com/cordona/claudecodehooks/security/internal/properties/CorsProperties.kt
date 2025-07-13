package com.cordona.claudecodehooks.security.internal.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.security.cors")
data class CorsProperties(
    val maxAge: Long,
    val allowCredentials: Boolean,
    val allowedHeaders: List<String>,
    val allowedMethods: List<String>,
    val allowedOrigins: List<String>
)