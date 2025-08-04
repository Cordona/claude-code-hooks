package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.event

import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import com.cordona.claudecodehooks.web.internal.rest.error.ExceptionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RequestPredicates.POST
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class StopEventRouter(
	private val properties: EndpointProperties,
	private val handler: StopEventHandler,
	private val exceptionFilter: ExceptionFilter,
) {

	@Bean
	fun stopEventEndpoint(): RouterFunction<ServerResponse> =
		RouterFunctions
			.route(POST(properties.claudeCode.hooks.stop.event), handler)
			.filter(exceptionFilter.handleException())
}