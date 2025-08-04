package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseWebTest
import com.cordona.claudecodehooks.config.TestSecurityConfig.Companion.TEST_USER_EXTERNAL_ID
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.shared.enums.Role.USER
import com.cordona.claudecodehooks.utils.JwtTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT

class SseConnectionTerminationTest : BaseWebTest() {

	@Autowired
	private lateinit var connectionStore: ConnectionStore

	@BeforeEach
	fun setUpSseConnection() {
		setupSseConnection()
	}

	@AfterEach
	fun tearDownSse() {
		tearDownSseConnection()
	}

	@Test
	fun `Should successfully disconnect user's own connection`() {
		val userConnectionsBefore = connectionStore.getUserConnections(TEST_USER_EXTERNAL_ID)

		assertThat(userConnectionsBefore)
			.`as`("User should have connections before disconnect")
			.isNotNull()
			.isNotEmpty()

		val connectionId = userConnectionsBefore!!.first().connectionId
		val totalConnectionsBefore = connectionStore.getConnectionsCount()

		val token = JwtTestUtils.createJwtAuthenticationToken(TEST_USER_EXTERNAL_ID, USER)

		mvcTestUtils.assertDeleteWithAuthentication(
			expectedStatus = NO_CONTENT,
			endpoint = "${endpointProperties.claudeCode.hooks.events.stream.disconnect}/$connectionId",
			authentication = token
		)

		val userConnectionsAfter = connectionStore.getUserConnections(TEST_USER_EXTERNAL_ID)
		val totalConnectionsAfter = connectionStore.getConnectionsCount()

		val disconnectedConnection = userConnectionsAfter?.find { it.connectionId == connectionId }

		assertThat(disconnectedConnection)
			.`as`("The specific connection should have been removed")
			.isNull()

		assertThat(totalConnectionsAfter)
			.`as`("Total connection count should decrease")
			.isLessThan(totalConnectionsBefore)
	}

	@Test
	fun `Should return NOT_FOUND for not existing connection ID`() {
		val invalidConnectionId = "non-existent-connection-id"

		val token = JwtTestUtils.createJwtAuthenticationToken(TEST_USER_EXTERNAL_ID, USER)

		mvcTestUtils.assertDeleteWithAuthentication(
			expectedStatus = NOT_FOUND,
			endpoint = "${endpointProperties.claudeCode.hooks.events.stream.disconnect}/$invalidConnectionId",
			authentication = token
		)
	}

	@Test
	fun `Should return FORBIDDEN when user tries to disconnect another user's connection`() {
		sseAssertions.assertConnectionEstablished(sseClient)

		val userConnections = connectionStore.getUserConnections(TEST_USER_EXTERNAL_ID)
		val connectionId = userConnections!!.first().connectionId

		val differentUserId = "different-user-12345"
		val differentUserToken = JwtTestUtils.createJwtAuthenticationToken(differentUserId, USER)

		mvcTestUtils.assertDeleteWithAuthentication(
			expectedStatus = FORBIDDEN,
			endpoint = "${endpointProperties.claudeCode.hooks.events.stream.disconnect}/$connectionId",
			authentication = differentUserToken
		)

		val userConnectionsAfter = connectionStore.getUserConnections(TEST_USER_EXTERNAL_ID)

		assertThat(userConnectionsAfter)
			.`as`("Original user's connection should still exist")
			.isNotNull()
			.isNotEmpty()
			.hasSize(1)
	}
}