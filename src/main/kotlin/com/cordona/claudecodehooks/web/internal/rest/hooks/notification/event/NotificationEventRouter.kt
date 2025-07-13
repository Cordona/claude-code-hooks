package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.event

import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RequestPredicates.POST
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class NotificationEventRouter(
	private val properties: EndpointProperties,
	private val handler: NotificationEventHandler
) {

	@Bean
	fun notificationEventEndpoint(): RouterFunction<ServerResponse> =
		RouterFunctions.route(POST(properties.claudeCode.hooks.notification.event), handler)
}