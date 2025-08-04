package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseWebTest
import com.cordona.claudecodehooks.persistence.internal.mongo.repositories.UserRepository
import com.cordona.claudecodehooks.shared.enums.Role.ADMIN
import com.cordona.claudecodehooks.shared.enums.Role.USER
import com.cordona.claudecodehooks.utils.JwtTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.NO_CONTENT

class InitializeUserTest : BaseWebTest() {

	@Autowired
	private lateinit var userRepository: UserRepository

	@Test
	fun `should create new user`() {
		val keycloakId = "3d4a0689-4514-4728-adc4-7a95c3c81344"
		val token = JwtTestUtils.createJwtAuthenticationToken(keycloakId, USER)

		mvcTestUtils.assertStatusWithAuthentication(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.user.initialize,
			payload = NO_PAYLOAD,
			authentication = token
		)

		val createdUser = userRepository.findByExternalId(keycloakId)

		assertThat(createdUser).isNotNull
		assertThat(createdUser!!.externalId).isEqualTo(keycloakId)
		assertThat(createdUser.roles).containsExactly(USER)
	}

	@Test
	fun `should not create duplicate user for same keycloakId`() {
		val keycloakId = "7b8c1234-5678-4abc-9def-123456789abc"
		val token = JwtTestUtils.createJwtAuthenticationToken(keycloakId, USER)

		// First call - creates user
		mvcTestUtils.assertStatusWithAuthentication(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.user.initialize,
			payload = NO_PAYLOAD,
			authentication = token
		)

		val userCountAfterFirst = userRepository.count()
		val firstUser = userRepository.findByExternalId(keycloakId)
		assertThat(firstUser).isNotNull

		// Second call - should return existing user, not create new one
		mvcTestUtils.assertStatusWithAuthentication(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.user.initialize,
			payload = NO_PAYLOAD,
			authentication = token
		)

		val userCountAfterSecond = userRepository.count()
		val secondUser = userRepository.findByExternalId(keycloakId)

		// Assert no new user was created
		assertThat(userCountAfterSecond).isEqualTo(userCountAfterFirst)
		assertThat(secondUser).isNotNull
		assertThat(secondUser!!.externalId).isEqualTo(keycloakId)
		assertThat(secondUser.roles).containsExactly(USER)
	}

	@Test
	fun `should create user with multiple roles`() {
		val keycloakId = "multi-role-user-12345"
		val roles = setOf(USER, ADMIN)
		val token = JwtTestUtils.createJwtAuthenticationToken(keycloakId, roles)

		mvcTestUtils.assertStatusWithAuthentication(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.user.initialize,
			payload = NO_PAYLOAD,
			authentication = token
		)

		val createdUser = userRepository.findByExternalId(keycloakId)

		assertThat(createdUser).isNotNull
		assertThat(createdUser!!.externalId).isEqualTo(keycloakId)
		assertThat(createdUser.roles).containsExactlyInAnyOrder(USER, ADMIN)
	}

	companion object {
		private const val NO_PAYLOAD = "{}"
	}
}