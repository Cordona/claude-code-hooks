package com.cordona.claudecodehooks.security.internal.services

import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyAuthenticationToken
import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class SecurityContextUserExternalIdResolver : UserExternalIdResolver {
	
	override fun execute(): String {
		val authentication = SecurityContextHolder.getContext().authentication
		return when (authentication) {
			is ApiKeyAuthenticationToken -> {
				val principal = authentication.principal as ApiKeyPrincipal
				principal.userExternalId
			}
			is JwtAuthenticationToken -> {
				authentication.token.subject
			}
			else -> throw IllegalArgumentException(
				"Expected ApiKeyAuthenticationToken or JwtAuthenticationToken but got ${authentication?.javaClass?.simpleName}"
			)
		}
	}
}