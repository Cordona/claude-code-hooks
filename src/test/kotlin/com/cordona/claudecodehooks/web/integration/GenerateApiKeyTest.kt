package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseWebTest
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.ApiKeyRepository
import com.cordona.claudecodehooks.shared.enums.Role.USER
import com.cordona.claudecodehooks.utils.JwtTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK

class GenerateApiKeyTest : BaseWebTest() {

	@Autowired
	private lateinit var apiKeyRepository: ApiKeyRepository

	@Test
	fun `should generate API key for existing user`() {
		val keycloakId = "3d4a0689-4514-4728-adc4-7a95c3c81344"
		val token = JwtTestUtils.createJwtAuthenticationToken(keycloakId, USER)

		mvcTestUtils.assertStatusWithAuthentication(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.user.initialize,
			payload = "{}",
			authentication = token
		)

		val requestPayload = fileTestUtils.fileToString(SOURCE_DIR, "generate_api_key_request.json")

		val response = mvcTestUtils.assertStatusWithAuthenticationAndReturn(
			expectedStatus = OK,
			endpoint = endpointProperties.claudeCode.developer.apiKey.generate,
			payload = requestPayload,
			authentication = token
		)

		val expected = fileTestUtils.fileToString(SOURCE_DIR, "expected_generate_api_key_response.json")

		jsonTestUtils.jsonAssertWithIgnoredFields(
			response,
			expected,
			"apiKey", "keyId", "keyPrefix", "lookupHash", "createdAt", "name"
		)

		assertThat(apiKeyRepository.findAll()).hasSize(1)
	}

	@Test
	fun `should return NOT_FOUND when user does not exist`() {
		val keycloakId = "non-existent-user-id"
		val token = JwtTestUtils.createJwtAuthenticationToken(keycloakId, USER)

		val requestPayload = fileTestUtils.fileToString(SOURCE_DIR, "generate_api_key_request.json")

		val response = mvcTestUtils.assertStatusWithAuthenticationAndReturn(
			expectedStatus = NOT_FOUND,
			endpoint = endpointProperties.claudeCode.developer.apiKey.generate,
			payload = requestPayload,
			authentication = token
		)

		val expected = fileTestUtils.fileToString(SOURCE_DIR, "expected_user_not_found_error_response.json")

		jsonTestUtils.jsonAssertWithIgnoredFields(
			response,
			expected,
			"message",
			"timestamp"
		)
	}

	companion object {
		private const val SOURCE_DIR = "developer/apikey"
	}
}