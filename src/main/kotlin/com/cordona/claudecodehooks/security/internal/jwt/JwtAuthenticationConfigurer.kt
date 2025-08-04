package com.cordona.claudecodehooks.security.internal.jwt

import com.cordona.claudecodehooks.shared.constants.JwtClaims.REALM_ACCESS
import com.cordona.claudecodehooks.shared.constants.JwtClaims.ROLES
import com.cordona.claudecodehooks.shared.enums.Role
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationConfigurer {

	fun configureJwtAuthentication(oauth2: OAuth2ResourceServerConfigurer<HttpSecurity>) {
		oauth2.jwt { jwt ->
			jwt.jwtAuthenticationConverter { token ->
				val roles = extractRolesFromJwt(token)
				val authorities = roles.map { role ->
					SimpleGrantedAuthority("$ROLE_PREFIX${role.value.uppercase()}")
				}
				JwtAuthenticationToken(token, authorities)
			}
		}
	}

	private fun extractRolesFromJwt(token: Jwt): Set<Role> {
		val realmAccess = token.getClaim<Map<String, Any>>(REALM_ACCESS)
		val keycloakRoles = realmAccess?.get(ROLES) as? List<*>

		val mappedRoles = keycloakRoles?.mapNotNull { keycloakRole ->
			when (keycloakRole.toString().lowercase()) {
				"admin" -> Role.ADMIN
				"user" -> Role.USER
				else -> null
			}
		}?.toSet() ?: emptySet()

		return mappedRoles.ifEmpty { setOf(Role.USER) }
	}

	companion object {
		private const val ROLE_PREFIX = "ROLE_"
	}
}