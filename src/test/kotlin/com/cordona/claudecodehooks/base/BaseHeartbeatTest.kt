package com.cordona.claudecodehooks.base

import com.cordona.claudecodehooks.config.TestSecurityConfig
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching.ConnectionStore
import com.cordona.claudecodehooks.utils.SseTestClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class BaseHeartbeatTest : BaseWebTest() {

	@Autowired
	protected lateinit var connectionStore: ConnectionStore

	protected val connections = mutableListOf<SseTestClient>()

	@BeforeEach
	fun setUpMultiConnections() {
		// Create 9 connections for the same test user (simulating multiple browser tabs)
		repeat(9) {
			val client = createSseTestClient()
			client.setupAndStart()
			connections.add(client)
		}

		// Verify all connections are established
		Assertions.assertThat(connections).hasSize(9)

		// Wait a moment for connections to be registered in cache
		Thread.sleep(100)

		// Verify connections are stored in cache
		val userConnections = connectionStore.getUserConnections(TestSecurityConfig.Companion.TEST_USER_EXTERNAL_ID)

		Assertions.assertThat(userConnections)
			.`as`("Test user should have connections in cache")
			.isNotNull()
			.hasSize(9)
	}

	@AfterEach
	fun tearDownMultiConnections() {
		// Stop all connections
		connections.forEach { it.stopSafely() }

		// Clear connection list
		connections.clear()
	}

	private fun createSseTestClient(): SseTestClient {
		return SseTestClient.Companion.create(
			serverPort = serverPort,
			sseEndpoint = endpointProperties.claudeCode.hooks.events.stream.connect,
			restClientBuilder = restClientBuilder,
			sseAssertions = sseAssertions,
			authHeaders = emptyMap() // Use default test authentication
		)
	}

	protected fun getAllConnections(): List<SseTestClient> {
		return connections
	}

	protected fun simulateConnectionDeath(clients: List<SseTestClient>) {
		clients.forEach { client ->
			// Simulate connection death by stopping the client abruptly
			client.stop()
		}
	}

	companion object {
		val HEARTBEAT_TIMEOUT: Duration = Duration.ofSeconds(2)
		val MULTI_HEARTBEAT_TIMEOUT: Duration = Duration.ofSeconds(5)
	}
}