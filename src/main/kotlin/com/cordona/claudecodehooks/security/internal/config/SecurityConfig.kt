package com.cordona.claudecodehooks.security.internal.config

import com.cordona.claudecodehooks.security.internal.apikey.ApiKeyAuthenticationFilter
import com.cordona.claudecodehooks.security.internal.apikey.ApiKeyAuthenticator
import com.cordona.claudecodehooks.security.internal.apikey.ApiKeyValidationUtils
import com.cordona.claudecodehooks.security.internal.jwt.JwtAuthenticationConfigurer
import com.cordona.claudecodehooks.security.internal.properties.SecurityProperties
import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
	private val securityProperties: SecurityProperties,
	private val endpointProperties: EndpointProperties,
	private val jwtAuthenticationConfigurer: JwtAuthenticationConfigurer,
) {

	@Bean
	@Order(1)
	fun jwtSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		return http
			.apply(::useCommonSecurity)
			.securityMatcher(
				endpointProperties.claudeCode.user.initialize,
				endpointProperties.claudeCode.hooks.events.stream.connect,
				endpointProperties.claudeCode.hooks.events.stream.disconnect + "/**",
				endpointProperties.claudeCode.developer.apiKey.generate
			)
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(endpointProperties.claudeCode.user.initialize).authenticated()
					.requestMatchers(endpointProperties.claudeCode.hooks.events.stream.connect).authenticated()
					.requestMatchers(endpointProperties.claudeCode.hooks.events.stream.disconnect + "/**").authenticated()
					.requestMatchers(endpointProperties.claudeCode.developer.apiKey.generate).authenticated()
					.anyRequest().denyAll()
			}
			.oauth2ResourceServer(jwtAuthenticationConfigurer::configureJwtAuthentication)
			.build()
	}

	@Bean
	@Order(2)
	fun apiKeySecurityFilterChain(
		http: HttpSecurity,
		apiKeyAuthenticator: ApiKeyAuthenticator,
		validationUtils: ApiKeyValidationUtils,
	): SecurityFilterChain {
		val apiKeyAuthenticationFilter = ApiKeyAuthenticationFilter(apiKeyAuthenticator, validationUtils)

		return http
			.apply(::useCommonSecurity)
			.securityMatcher(
				endpointProperties.claudeCode.hooks.stop.event,
				endpointProperties.claudeCode.hooks.notification.event
			)
			.addFilterBefore(apiKeyAuthenticationFilter, BasicAuthenticationFilter::class.java)
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers(endpointProperties.claudeCode.hooks.stop.event).authenticated()
					.requestMatchers(endpointProperties.claudeCode.hooks.notification.event).authenticated()
					.anyRequest().denyAll()
			}
			.build()
	}

	@Bean
	@Order(3)
	fun publicSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		return http
			.apply(::useCommonSecurity)
			.securityMatcher("${endpointProperties.actuator.base}/**")
			.authorizeHttpRequests { auth ->
				auth
					.requestMatchers("${endpointProperties.actuator.base}/**").permitAll()
			}
			.build()
	}

	private fun useCommonSecurity(http: HttpSecurity): HttpSecurity {
		return http
			.csrf { it.disable() }
			.cors { it.configurationSource(corsConfigurationSource()) }
			.sessionManagement { it.sessionCreationPolicy(STATELESS) }
	}

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration().apply {
			allowedOrigins = securityProperties.cors.allowedOrigins
			allowedMethods = securityProperties.cors.allowedMethods
			allowedHeaders = securityProperties.cors.allowedHeaders
			allowCredentials = securityProperties.cors.allowCredentials
			maxAge = securityProperties.cors.maxAgeSeconds
			exposedHeaders = listOf(CONTENT_TYPE, CACHE_CONTROL)
		}

		return UrlBasedCorsConfigurationSource().apply {
			securityProperties.cors.enabledPaths.forEach { path ->
				registerCorsConfiguration(path, configuration)
			}
		}
	}
}