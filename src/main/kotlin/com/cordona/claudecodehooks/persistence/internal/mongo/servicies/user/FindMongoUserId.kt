package com.cordona.claudecodehooks.persistence.internal.mongo.servicies.user

import com.cordona.claudecodehooks.persistence.external.api.FindUserId
import com.cordona.claudecodehooks.persistence.internal.mongo.annotations.ConditionalOnMongoService
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.UserRepository
import org.springframework.stereotype.Service

@Service
@ConditionalOnMongoService
class FindMongoUserId(
	private val userRepository: UserRepository,
) : FindUserId {

	override fun execute(externalId: String): String? = userRepository.findByExternalId(externalId)?.id
}