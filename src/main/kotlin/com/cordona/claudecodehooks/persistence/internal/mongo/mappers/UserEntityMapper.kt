package com.cordona.claudecodehooks.persistence.internal.mongo.mappers

import com.cordona.claudecodehooks.persistence.internal.mongo.entities.UserEntity
import com.cordona.claudecodehooks.shared.models.User
import org.springframework.stereotype.Component

@Component
class UserEntityMapper {

	fun toModel(entity: UserEntity): User {
		return User(
			id = entity.id,
			externalId = entity.externalId,
			roles = entity.roles
		)
	}
}