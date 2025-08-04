package com.cordona.claudecodehooks.web.internal.rest.developer.apikey

import com.cordona.claudecodehooks.shared.models.ApiKey
import java.time.Instant

data class GenerateApiKeyResponse(
	val apiKey: String,
	val keyId: String,
	val keyPrefix: String,
	val lookupHash: String,
	val createdAt: Instant,
	val lastUsedAt: Instant? = null,
	val isActive: Boolean = true,
	val name: String? = null,
	val permissions: List<String>? = null,
	val expiresAt: Instant? = null,
) {
	companion object {
		fun from(apiKey: ApiKey): GenerateApiKeyResponse {
			return GenerateApiKeyResponse(
				apiKey = apiKey.apiKey ?: throw IllegalStateException("API key not available for response"),
				keyId = apiKey.keyId,
				keyPrefix = apiKey.keyPrefix,
				lookupHash = apiKey.lookupHash,
				createdAt = apiKey.createdAt,
				lastUsedAt = apiKey.lastUsedAt,
				isActive = apiKey.isActive,
				name = apiKey.name,
				permissions = apiKey.permissions.map { it.value },
				expiresAt = apiKey.expiresAt
			)
		}
	}
}