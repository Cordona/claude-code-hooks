package com.cordona.claudecodehooks.persistence.internal.mongo.mappers

import com.cordona.claudecodehooks.persistence.internal.mongo.entities.ApiKeyEntity
import com.cordona.claudecodehooks.shared.models.ApiKey
import org.springframework.stereotype.Component

@Component
class ApiKeyEntityMapper {

	fun toModel(entity: ApiKeyEntity): ApiKey {
		return ApiKey(
			keyId = entity.id,
			keyPrefix = entity.keyPrefix,
			lookupHash = entity.lookupHash,
			userId = entity.userId,
			userExternalId = entity.userExternalId,
			createdAt = entity.createdAt!!,
			lastUsedAt = entity.lastUsedAt,
			isActive = entity.isActive,
			name = entity.name,
			permissions = entity.permissions,
			expiresAt = entity.expiresAt,
			apiKey = null
		)
	}
}