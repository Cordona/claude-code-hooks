package com.cordona.claudecodehooks.persistence.internal.mongo.servicies.apikey

import com.cordona.claudecodehooks.persistence.external.api.UpdateApiKeyLastUsed
import com.cordona.claudecodehooks.persistence.internal.mongo.annotations.ConditionalOnMongoService
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.ApiKeyRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
@ConditionalOnMongoService
class UpdateMongoApiKeyLastUsed(
	private val apiKeyRepository: ApiKeyRepository,
) : UpdateApiKeyLastUsed {

	override fun execute(keyId: String, lastUsedAt: Instant) = apiKeyRepository.updateLastUsedAt(keyId, lastUsedAt)
}