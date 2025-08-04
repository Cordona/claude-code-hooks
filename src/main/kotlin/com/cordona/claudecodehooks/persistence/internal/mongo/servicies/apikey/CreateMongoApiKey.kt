package com.cordona.claudecodehooks.persistence.internal.mongo.servicies.apikey

import com.cordona.claudecodehooks.persistence.external.api.CreateApiKey
import com.cordona.claudecodehooks.persistence.internal.mongo.annotations.ConditionalOnMongoService
import com.cordona.claudecodehooks.persistence.internal.mongo.entities.ApiKeyEntity
import com.cordona.claudecodehooks.persistence.internal.mongo.mappers.ApiKeyEntityMapper
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.ApiKeyRepository
import com.cordona.claudecodehooks.shared.enums.Permission
import com.cordona.claudecodehooks.shared.models.ApiKey
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@ConditionalOnMongoService
class CreateMongoApiKey(
	private val apiKeyRepository: ApiKeyRepository,
	private val apiKeyEntityMapper: ApiKeyEntityMapper,
) : CreateApiKey {

	override fun execute(
		lookupHash: String,
		userId: String,
		userExternalId: String,
		keyPrefix: String,
		name: String,
		permissions: List<Permission>,
		expiresAt: Instant?,
	): ApiKey {
		val entity = ApiKeyEntity(
			lookupHash = lookupHash,
			userId = userId,
			userExternalId = userExternalId,
			keyPrefix = keyPrefix,
			name = name,
			isActive = true,
			permissions = permissions,
			expiresAt = expiresAt
		)

		val saved = apiKeyRepository.save(entity)

		return apiKeyEntityMapper.toModel(saved)
	}
}