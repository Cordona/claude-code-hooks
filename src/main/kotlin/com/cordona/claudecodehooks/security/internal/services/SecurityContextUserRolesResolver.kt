package com.cordona.claudecodehooks.security.internal.services

import com.cordona.claudecodehooks.security.external.api.UserRolesResolver
import com.cordona.claudecodehooks.shared.enums.Role
import com.cordona.claudecodehooks.shared.enums.Role.ADMIN
import com.cordona.claudecodehooks.shared.enums.Role.USER
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class SecurityContextUserRolesResolver : UserRolesResolver {

	override fun execute(): Set<Role> {
		val authentication = SecurityContextHolder.getContext().authentication
		require(authentication is JwtAuthenticationToken) {
			"Expected JwtAuthenticationToken but got ${authentication?.javaClass?.simpleName}"
		}

		return authentication.authorities
			.filter { it.authority.startsWith("ROLE_") }
			.mapNotNull { authority ->
				when (authority.authority) {
					"ROLE_ADMIN" -> ADMIN
					"ROLE_USER" -> USER
					else -> null
				}
			}
			.toSet()
			.takeIf { it.isNotEmpty() } ?: setOf(USER)
	}
}