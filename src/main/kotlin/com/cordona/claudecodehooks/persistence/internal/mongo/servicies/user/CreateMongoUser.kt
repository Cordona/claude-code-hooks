package com.cordona.claudecodehooks.persistence.internal.mongo.servicies.user

import com.cordona.claudecodehooks.persistence.external.api.CreateUser
import com.cordona.claudecodehooks.persistence.internal.mongo.annotations.ConditionalOnMongoService
import com.cordona.claudecodehooks.persistence.internal.mongo.entities.UserEntity
import com.cordona.claudecodehooks.persistence.internal.mongo.mappers.UserEntityMapper
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.UserRepository
import com.cordona.claudecodehooks.shared.enums.Role
import com.cordona.claudecodehooks.shared.models.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
@ConditionalOnMongoService
class CreateMongoUser(
	private val userRepository: UserRepository,
	private val userEntityMapper: UserEntityMapper,
) : CreateUser {

	private val logger = KotlinLogging.logger {}

	override fun execute(externalId: String, roles: Set<Role>): User {
		val existing = userRepository.findByExternalId(externalId)

		if (existing != null) {
			logger.info { "User with external ID $externalId already exists" }
			return userEntityMapper.toModel(existing)
		}

		val new = UserEntity(
			externalId = externalId,
			roles = roles
		)

		val saved = userRepository.save(new)

		logger.info { "Created new user with external ID: $externalId, roles: $roles" }

		return userEntityMapper.toModel(saved)
	}
}