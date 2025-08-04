package com.cordona.claudecodehooks.security.internal.apikey

import com.cordona.claudecodehooks.persistence.external.api.FindApiKeyByLookupHash
import com.cordona.claudecodehooks.persistence.external.api.UpdateApiKeyLastUsed
import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyAuthenticationToken
import com.cordona.claudecodehooks.shared.enums.Permission
import com.cordona.claudecodehooks.shared.utils.CryptoUtils
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ApiKeyAuthenticator(
	private val findApiKeyByLookupHash: FindApiKeyByLookupHash,
	private val updateApiKeyLastUsed: UpdateApiKeyLastUsed,
) {

	fun authenticate(apiKey: String): ApiKeyAuthenticationToken? {
		val lookupHash = CryptoUtils.generateLookupHash(apiKey)
		val retrieved = findApiKeyByLookupHash.execute(lookupHash) ?: return null

		if (retrieved.isInvalid()) {
			return null
		}

		updateApiKeyLastUsed.execute(retrieved.keyId, Instant.now())

		val authorities = createAuthorities(retrieved.permissions)

		return ApiKeyAuthenticationToken(
			keyId = retrieved.keyId,
			userId = retrieved.userId,
			userExternalId = retrieved.userExternalId,
			keyPrefix = retrieved.keyPrefix,
			permissions = retrieved.permissions,
			authorities = authorities
		)
	}

	private fun createAuthorities(permissions: List<Permission>): Collection<GrantedAuthority> {
		return permissions.map { permission ->
			SimpleGrantedAuthority("$PERMISSION_PREFIX${permission.value.uppercase().replace(":", "_")}")
		}
	}

	companion object {
		private const val PERMISSION_PREFIX = "PERMISSION_"
	}
}