package com.cordona.claudecodehooks.persistence.internal.mongo.repositories

import com.cordona.claudecodehooks.persistence.internal.mongo.entities.ApiKeyEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import java.time.Instant
import java.util.Optional

interface ApiKeyRepository : MongoRepository<ApiKeyEntity, String> {

	fun findByLookupHash(lookupHash: String): Optional<ApiKeyEntity>

	@Query("{ '_id': ?0 }")
	@Update("{ '\$set': { 'lastUsedAt': ?1 } }")
	fun updateLastUsedAt(keyId: String, lastUsedAt: Instant)
}