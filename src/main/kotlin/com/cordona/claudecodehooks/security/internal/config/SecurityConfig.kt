package com.cordona.claudecodehooks.security.internal.config

import com.cordona.claudecodehooks.security.internal.properties.CorsProperties
import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class SecurityConfig(
	private val corsProperties: CorsProperties,
	private val endpointProperties: EndpointProperties,
) {

	@Bean
	@Profile("local", "docker")
	fun corsConfigurer(): WebMvcConfigurer {
		return object : WebMvcConfigurer {
			override fun addCorsMappings(registry: CorsRegistry) {
				val sseStreamPath = endpointProperties.claudeCode.hooks.events.stream

				registry
					.addMapping(sseStreamPath)
					.allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
					.allowedMethods(*corsProperties.allowedMethods.toTypedArray())
					.allowedHeaders(*corsProperties.allowedHeaders.toTypedArray())
					.allowCredentials(corsProperties.allowCredentials)
					.maxAge(corsProperties.maxAge)
					.exposedHeaders(CONTENT_TYPE, CACHE_CONTROL)
			}
		}
	}
}