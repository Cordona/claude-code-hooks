package com.cordona.claudecodehooks.web.internal.rest.error

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.HandlerFilterFunction
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class ExceptionFilter(
    private val exceptionHandler: ExceptionHandler,
) {

	@Bean
	fun handleException(): HandlerFilterFunction<ServerResponse, ServerResponse> {
		return HandlerFilterFunction { request, next ->
			try {
				next.handle(request)
			} catch (exception: Exception) {
				exceptionHandler.handle(exception)
			}
		}
	}
}