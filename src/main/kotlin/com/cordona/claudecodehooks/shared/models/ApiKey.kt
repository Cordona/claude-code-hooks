package com.cordona.claudecodehooks.shared.models

import com.cordona.claudecodehooks.shared.enums.Permission
import java.time.Instant

data class ApiKey(
	val keyId: String,
	val keyPrefix: String,
	val lookupHash: String,
	val userId: String,
	val userExternalId: String,
	val createdAt: Instant,
	val lastUsedAt: Instant? = null,
	val isActive: Boolean = true,
	val name: String? = null,
	val permissions: List<Permission> = listOf(Permission.HOOKS_WRITE),
	val expiresAt: Instant? = null,
	val apiKey: String? = null,
) {
	fun isInvalid(): Boolean {
		return !isActive || isExpired()
	}

	fun isExpired(): Boolean {
		return expiresAt?.let { expiresAt ->
			Instant.now().isAfter(expiresAt)
		} ?: false
	}
}