package com.cordona.claudecodehooks.persistence.internal.mongo.repositories

import com.cordona.claudecodehooks.persistence.internal.mongo.entities.UserEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<UserEntity, String> {
	fun findByExternalId(externalId: String): UserEntity?
}