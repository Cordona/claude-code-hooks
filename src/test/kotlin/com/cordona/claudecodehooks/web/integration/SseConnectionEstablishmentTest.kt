package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseWebTest
import com.cordona.claudecodehooks.config.TestSecurityConfig.Companion.TEST_USER_EXTERNAL_ID
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection.ConnectionConfirmation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SseConnectionEstablishmentTest : BaseWebTest() {

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
	fun `Should establish SSE connection and store in cache`() {
		sseAssertions.assertConnectionEstablished(sseClient)

		val userConnections = connectionStore.getUserConnections(TEST_USER_EXTERNAL_ID)

		assertThat(userConnections)
			.`as`("User should have connections in cache")
			.isNotNull()
			.isNotEmpty()

		val connectionEvents = sseClient.waitForConnectionEvents(DEFAULT_SSE_TIMEOUT, expectedCount = 1)

		assertThat(connectionEvents)
			.`as`("Should receive connection confirmation event")
			.hasSize(1)

		val connectionEventData = connectionEvents.first()
		val connectionConfirmation = serialTestUtils.deserialize(connectionEventData, ConnectionConfirmation::class.java)

		assertThat(connectionConfirmation.message)
			.`as`("Connection confirmation message should be correct")
			.isEqualTo("Connected to Claude Code Hooks")

		assertThat(connectionConfirmation.connectionId)
			.`as`("Connection ID should not be null or empty")
			.isNotNull()
			.isNotEmpty()
	}
}