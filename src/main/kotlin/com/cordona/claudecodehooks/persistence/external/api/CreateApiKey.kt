package com.cordona.claudecodehooks.persistence.external.api

import com.cordona.claudecodehooks.shared.enums.Permission
import com.cordona.claudecodehooks.shared.models.ApiKey
import java.time.Instant

fun interface CreateApiKey {
	fun execute(
		lookupHash: String,
		userId: String,
		userExternalId: String,
		keyPrefix: String,
		name: String,
		permissions: List<Permission>,
		expiresAt: Instant?,
	): ApiKey
}