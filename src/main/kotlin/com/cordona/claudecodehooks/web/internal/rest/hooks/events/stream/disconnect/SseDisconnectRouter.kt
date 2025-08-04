package com.cordona.claudecodehooks.web.internal.rest.hooks.events.stream.disconnect

import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import com.cordona.claudecodehooks.web.internal.rest.error.ExceptionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RequestPredicates.DELETE
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class SseDisconnectRouter(
	private val properties: EndpointProperties,
	private val handler: SseDisconnectHandler,
	private val exceptionFilter: ExceptionFilter,
) {

	@Bean
	fun sseDisconnectEndpoint(): RouterFunction<ServerResponse> =
		RouterFunctions
			.route(DELETE("${properties.claudeCode.hooks.events.stream.disconnect}/{connectionId}"), handler)
			.filter(exceptionFilter.handleException())
}