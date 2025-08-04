package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseHeartbeatTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HeartbeatDeliveryTest : BaseHeartbeatTest() {

	@Test
	fun `Should deliver heartbeats to all connections independently`() {
		// Given: 9 connections are established (done in setup)
		val allConnections = getAllConnections()
		assertThat(allConnections).hasSize(9)

		// When: Wait for multiple heartbeat cycles,
		// Each connection should receive heartbeats independently
		val expectedHeartbeatsPerConnection = 2

		// Then: All connections receive heartbeats
		allConnections.forEach { connection ->
			val heartbeats = connection.waitForHeartbeats(
				timeout = MULTI_HEARTBEAT_TIMEOUT,
				expectedCount = expectedHeartbeatsPerConnection
			)

			assertThat(heartbeats)
				.`as`("Connection should receive expected number of heartbeats")
				.hasSize(expectedHeartbeatsPerConnection)

			heartbeats.forEach { heartbeat ->
				assertThat(heartbeat)
					.`as`("Heartbeat content should be correct")
					.isEqualTo("heartbeat")
			}
		}
	}

	@Test
	fun `Should maintain connection isolation for multiple connections`() {
		// Given: 9 connections for the same user (simulating multiple browser tabs)
		val allConnections = getAllConnections()
		assertThat(allConnections).hasSize(9)

		// When: Wait for heartbeats on all connections
		val expectedHeartbeatsPerConnection = 2
		val allHeartbeats = allConnections.map { connection ->
			connection.waitForHeartbeats(MULTI_HEARTBEAT_TIMEOUT, expectedHeartbeatsPerConnection)
		}

		// Then: Each connection receives its own independent heartbeats
		allHeartbeats.forEach { heartbeats ->
			assertThat(heartbeats)
				.`as`("Each connection should receive heartbeats independently")
				.hasSize(expectedHeartbeatsPerConnection)
		}

		// Verify heartbeat content
		allHeartbeats.flatten().forEach { heartbeat ->
			assertThat(heartbeat)
				.`as`("All heartbeats should have correct content")
				.isEqualTo("heartbeat")
		}

		// Verify total heartbeats received
		val totalHeartbeats = allHeartbeats.flatten().size
		assertThat(totalHeartbeats)
			.`as`("Total heartbeats should be 9 connections × 2 heartbeats each")
			.isEqualTo(18)
	}
}