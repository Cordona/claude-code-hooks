package com.cordona.claudecodehooks.utils

import com.cordona.claudecodehooks.shared.enums.Role
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

object JwtTestUtils {

	fun createJwtAuthenticationToken(
		keycloakExternalId: String,
		role: Role,
	): JwtAuthenticationToken = createJwtAuthenticationToken(keycloakExternalId, setOf(role))

	fun createJwtAuthenticationToken(
		keycloakExternalId: String,
		roles: Set<Role>,
	): JwtAuthenticationToken {
		val jwt = Jwt.withTokenValue("test-token")
			.header("alg", "RS256")
			.header("typ", "JWT")
			.subject(keycloakExternalId)
			.issuer("https://localhost:8443/realms/claude-code-hooks")
			.audience(listOf("claude-code-hooks"))
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(3600))
			.build()

		val authorities = roles.map { role ->
			SimpleGrantedAuthority("ROLE_${role.name}")
		}

		return JwtAuthenticationToken(jwt, authorities)
	}
}