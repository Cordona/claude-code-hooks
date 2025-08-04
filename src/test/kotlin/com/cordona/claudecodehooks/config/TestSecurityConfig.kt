package com.cordona.claudecodehooks.config

import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyAuthenticationToken
import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyPrincipal
import com.cordona.claudecodehooks.shared.constants.JwtClaims
import com.cordona.claudecodehooks.shared.enums.Role
import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import com.cordona.claudecodehooks.utils.JwtTestUtils
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig(
	private val endpointProperties: EndpointProperties,
) {

	@Bean
	@Order(1)
	fun testJwtSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		return http
			.securityMatcher(
				endpointProperties.claudeCode.user.initialize,
				endpointProperties.claudeCode.developer.apiKey.generate,
				endpointProperties.claudeCode.hooks.events.stream.connect,
				endpointProperties.claudeCode.hooks.events.stream.disconnect + "/**"
			)
			.addFilterBefore(
				testJwtAuthenticationFilter(),
				org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java
			)
			.authorizeHttpRequests { requests -> requests.anyRequest().permitAll() }
			.csrf { it.disable() }
			.build()
	}

	@Bean
	fun testJwtAuthenticationFilter(): OncePerRequestFilter {
		return object : OncePerRequestFilter() {
			override fun doFilterInternal(
				request: HttpServletRequest,
				response: HttpServletResponse,
				filterChain: jakarta.servlet.FilterChain,
			) {
				val sseConnectPathMatcher =
					AntPathRequestMatcher(endpointProperties.claudeCode.hooks.events.stream.connect)
				val sseDisconnectPathMatcher =
					AntPathRequestMatcher(endpointProperties.claudeCode.hooks.events.stream.disconnect + "/**")

				// Only set default authentication if none is already provided
				val currentAuth = SecurityContextHolder.getContext().authentication
				if ((sseConnectPathMatcher.matches(request) || sseDisconnectPathMatcher.matches(request)) &&
					(currentAuth == null || currentAuth.name == "anonymousUser")
				) {
					val jwtAuth = JwtTestUtils.createJwtAuthenticationToken(TEST_USER_EXTERNAL_ID, Role.USER)
					SecurityContextHolder.getContext().authentication = jwtAuth
				}

				filterChain.doFilter(request, response)
			}
		}
	}

	@Bean
	@Order(2)
	fun testPublicSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		return http
			.authorizeHttpRequests { requests -> requests.anyRequest().permitAll() }
			.csrf { it.disable() }
			.build()
	}

	@Bean("userExternalIdResolver")
	fun testUserExternalIdResolver(): UserExternalIdResolver {
		return UserExternalIdResolver {
			val authentication = SecurityContextHolder.getContext().authentication

			when (authentication) {
				is ApiKeyAuthenticationToken -> {
					val principal = authentication.principal as ApiKeyPrincipal
					principal.userExternalId
				}

				is JwtAuthenticationToken -> {
					val jwt = authentication.principal as Jwt
					jwt.getClaimAsString(JwtClaims.SUB)
				}

				is AnonymousAuthenticationToken -> {
					// For tests, use a default test user external ID
					TEST_USER_EXTERNAL_ID
				}

				else -> {
					throw IllegalArgumentException(
						"Expected ApiKeyAuthenticationToken, JwtAuthenticationToken, or AnonymousAuthenticationToken but got ${authentication?.javaClass?.simpleName}"
					)
				}
			}
		}
	}

	companion object {
		const val TEST_USER_EXTERNAL_ID = "00000000-0000-0000-0000-000000000001"
	}
}