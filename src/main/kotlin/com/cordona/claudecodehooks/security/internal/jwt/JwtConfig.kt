package com.cordona.claudecodehooks.security.internal.jwt

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

/**
 * Docker-specific JWT configuration that resolves Docker networking issues with Keycloak.
 *
 * **Problem**: JWT tokens issued by Keycloak contain issuer claim "http://localhost:8080/realms/claude-code-hooks"
 * which works for external clients (React app on host network), but Spring Boot app running inside Docker
 * container cannot reach "localhost:8080" to fetch JWKS for token validation.
 *
 * **Solution**: Configure custom JwtDecoder that:
 * - Accepts JWT tokens with external issuer claim (localhost:8080)
 * - Fetches JWKS from internal Docker network (keycloak:8080)
 * - Maintains proper JWT validation without DevJwtConfig complexity
 *
 * **Network Architecture**:
 * - React App (host): http://localhost:8080 → Issues JWT tokens
 * - Spring Boot (container): http://keycloak:8080 → Fetches JWKS for validation
 *
 * Only active when `spring.profiles.active=docker` to avoid affecting other environments.
 */
@Configuration
@ConditionalOnProperty("spring.profiles.active", havingValue = "docker")
class JwtConfig(
    private val properties: OAuth2ResourceServerProperties,
) {

	@Bean
	fun jwtDecoder(): JwtDecoder {
		val issuerUri = properties.jwt.issuerUri
		val internalJwksUri = buildInternalJwksUri(issuerUri)

		return NimbusJwtDecoder.withJwkSetUri(internalJwksUri).build()
	}

	private fun buildInternalJwksUri(issuerUri: String?): String {
		return when {
			issuerUri?.contains(EXTERNAL_KEYCLOAK_HOST) == true -> {
				issuerUri.replace(EXTERNAL_KEYCLOAK_HOST, INTERNAL_KEYCLOAK_HOST) + JWKS_ENDPOINT_PATH
			}

			else -> throw IllegalArgumentException("Expected localhost issuer URI but got: $issuerUri")
		}
	}

	companion object {
		private const val EXTERNAL_KEYCLOAK_HOST = "localhost:8080"
		private const val INTERNAL_KEYCLOAK_HOST = "keycloak:8080"
		private const val JWKS_ENDPOINT_PATH = "/protocol/openid-connect/certs"
	}
}