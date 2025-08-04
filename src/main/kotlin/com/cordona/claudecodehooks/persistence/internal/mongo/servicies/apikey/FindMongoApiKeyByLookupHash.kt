package com.cordona.claudecodehooks.persistence.internal.mongo.servicies.apikey

import com.cordona.claudecodehooks.persistence.external.api.FindApiKeyByLookupHash
import com.cordona.claudecodehooks.persistence.internal.mongo.annotations.ConditionalOnMongoService
import com.cordona.claudecodehooks.persistence.internal.mongo.mappers.ApiKeyEntityMapper
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.ApiKeyRepository
import com.cordona.claudecodehooks.shared.models.ApiKey
import org.springframework.stereotype.Service

@Service
@ConditionalOnMongoService
class FindMongoApiKeyByLookupHash(
	private val apiKeyRepository: ApiKeyRepository,
	private val apiKeyEntityMapper: ApiKeyEntityMapper,
) : FindApiKeyByLookupHash {

	override fun execute(lookupHash: String): ApiKey? =
		apiKeyRepository.findByLookupHash(lookupHash).map(apiKeyEntityMapper::toModel).orElse(null)
}