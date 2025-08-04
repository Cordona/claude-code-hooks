package com.cordona.claudecodehooks.shared.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.web.endpoints")
data class EndpointProperties(
	val claudeCode: ClaudeCode,
	val actuator: ActuatorEndpoints,
) {
	data class ActuatorEndpoints(
		val base: String,
		val health: String,
		val info: String,
	)

	data class ClaudeCode(
		val base: String,
		val hooks: Hooks,
		val user: User,
		val developer: Developer,
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
				val stream: Stream,
			) {
				data class Stream(
					val connect: String,
					val disconnect: String,
				)
			}
		}

		data class User(
			val initialize: String,
		)

		data class Developer(
			val apiKey: ApiKey,
		) {
			data class ApiKey(
				val generate: String,
			)
		}
	}
}