package com.cordona.claudecodehooks.security.internal.apikey.models

import com.cordona.claudecodehooks.shared.enums.Permission
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class ApiKeyAuthenticationToken(
	private val keyId: String,
	private val userId: String,
	private val userExternalId: String,
	private val keyPrefix: String,
	private val permissions: List<Permission>,
	authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {

	init {
		isAuthenticated = true
	}

	override fun getCredentials(): Any? = null

	override fun getPrincipal(): Any = ApiKeyPrincipal(
		keyId = keyId,
		userId = userId,
		userExternalId = userExternalId,
		keyPrefix = keyPrefix
	)

	fun hasPermission(requiredPermission: Permission): Boolean {
		return permissions.contains(requiredPermission)
	}

	fun doesNotHavePermission(requiredPermission: Permission): Boolean {
		return !hasPermission(requiredPermission)
	}
}