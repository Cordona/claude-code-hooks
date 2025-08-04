package com.cordona.claudecodehooks.domain.internal.developer.apikey

import com.cordona.claudecodehooks.domain.external.api.GenerateApiKey
import com.cordona.claudecodehooks.persistence.external.api.CreateApiKey
import com.cordona.claudecodehooks.persistence.external.api.FindUserId
import com.cordona.claudecodehooks.shared.commands.GenerateApiKeyCommand
import com.cordona.claudecodehooks.shared.exceptions.DataAccessException
import com.cordona.claudecodehooks.shared.exceptions.ExceptionDetails
import com.cordona.claudecodehooks.shared.exceptions.ExceptionUtils.logAndRaise
import com.cordona.claudecodehooks.shared.models.ApiKey
import com.cordona.claudecodehooks.shared.utils.CryptoUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class GenerateApiKeyService(
	private val findUserId: FindUserId,
	private val createApiKey: CreateApiKey,
) : GenerateApiKey {

	private val logger = KotlinLogging.logger {}

	override fun execute(command: GenerateApiKeyCommand): ApiKey {
		val userId = findUserId.execute(command.externalId)
			?: logAndRaise(
				::DataAccessException,
				ExceptionDetails(
					statusCode = NOT_FOUND,
					message = "User with external ID ${command.externalId} not found"
				),
				logger
			)

		val secureApiKey = generateSecureApiKey()
		val keyPrefix = "${secureApiKey.take(10)}..."
		val lookupHash = CryptoUtils.generateLookupHash(secureApiKey)

		val apiKey = createApiKey.execute(
			lookupHash = lookupHash,
			userId = userId,
			userExternalId = command.externalId,
			keyPrefix = keyPrefix,
			name = command.name,
			permissions = command.permissions,
			expiresAt = command.expiresAt
		)

		logger.info { "Generated API key with ID ${apiKey.keyId} for user with external ID: ${command.externalId}" }

		return apiKey.copy(apiKey = secureApiKey)
	}

	private fun generateSecureApiKey(): String {
		val keyGenerator = KeyGenerators.secureRandom(KEY_LENGTH_BYTES)
		val bytes = keyGenerator.generateKey()

		return "$API_KEY_PREFIX${Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)}"
	}

	companion object {
		private const val API_KEY_PREFIX = "chk_"
		private const val KEY_LENGTH_BYTES = 32
	}
}