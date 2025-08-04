package com.cordona.claudecodehooks.security.internal.apikey

import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyPrincipal
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthenticationFilter(
	private val apiKeyAuthenticator: ApiKeyAuthenticator,
	private val validationUtils: ApiKeyValidationUtils,
) : OncePerRequestFilter() {

	private val log = KotlinLogging.logger {}

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		try {
			val apiKey = request.getHeader(API_KEY_HEADER)?.takeIf { it.isNotBlank() }?.trim()

			if (apiKey == null) {
				log.warn { "No API key found in request to ${request.requestURI}" }
				response.status = UNAUTHORIZED.value()
				return
			}

			val authenticationToken = apiKeyAuthenticator.authenticate(apiKey)

			if (authenticationToken == null) {
				log.warn { "Invalid API key for request to ${request.requestURI}" }
				response.status = UNAUTHORIZED.value()
				return
			}

			if (validationUtils.doesNotHaveRequiredPermissions(request, authenticationToken)) {
				log.warn { "API key lacks required permissions for ${request.requestURI}" }
				response.status = FORBIDDEN.value()
				return
			}

			SecurityContextHolder.getContext().authentication = authenticationToken

			val principal = authenticationToken.principal as ApiKeyPrincipal

			log.debug {
				"Successfully authenticated API key ${principal.keyPrefix} for user with external ID ${principal.userExternalId}"
			}

			filterChain.doFilter(request, response)

		} catch (e: Exception) {
			log.error { "Error during API key authentication for ${request.requestURI}: ${e.message}" }
			response.status = INTERNAL_SERVER_ERROR.value()
		}
	}

	companion object {
		private const val API_KEY_HEADER = "X-API-Key"
	}
}