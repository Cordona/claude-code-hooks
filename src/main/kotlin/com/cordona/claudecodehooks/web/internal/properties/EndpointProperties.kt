package com.cordona.claudecodehooks.web.internal.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.web.endpoints")
data class EndpointProperties(
	val claudeCode: ClaudeCode,
) {
	data class ClaudeCode(
		val hooks: Hooks,
	) {
		data class Hooks(
			val base: String,
			val notification: Notification,
			val stop: Stop,
			val events: Events,
		) {
			data class Notification(
				val event: String,
			)
			data class Stop(
				val event: String,
			)
			data class Events(
				val stream: String,
			)
		}
	}
}