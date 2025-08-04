package com.cordona.claudecodehooks.web.internal.rest.developer.apikey

import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import com.cordona.claudecodehooks.web.internal.rest.error.ExceptionFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RequestPredicates.POST
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class GenerateApiKeyRouter(
	private val properties: EndpointProperties,
	private val handler: GenerateApiKeyHandler,
	private val exceptionFilter: ExceptionFilter,
) {

	@Bean
	fun generateApiKeyEndpoint(): RouterFunction<ServerResponse> =
		RouterFunctions
			.route(POST(properties.claudeCode.developer.apiKey.generate), handler)
			.filter(exceptionFilter.handleException())
}