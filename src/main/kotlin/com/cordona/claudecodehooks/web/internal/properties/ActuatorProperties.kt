package com.cordona.claudecodehooks.web.internal.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.actuator")
data class ActuatorProperties(
	val base: String,
	val health: String,
	val info: String,
)