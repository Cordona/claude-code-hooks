package com.cordona.claudecodehooks.security.internal.apikey.models

data class ApiKeyPrincipal(
	val keyId: String,
	val userId: String,
	val userExternalId: String,
	val keyPrefix: String,
) {
	override fun toString(): String =
		"ApiKey(keyId=$keyId, internalUserId=$userId, externalUserId=$userExternalId, keyPrefix=$keyPrefix)"
}