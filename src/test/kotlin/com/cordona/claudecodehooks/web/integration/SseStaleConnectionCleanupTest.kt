package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseHeartbeatTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class SseStaleConnectionCleanupTest : BaseHeartbeatTest() {

	@Test
	fun `Should continue delivering heartbeats to healthy connections while cleaning up stale ones`() {
		// Given: 9 connections are established
		val allConnections = getAllConnections()
		assertThat(allConnections).hasSize(9)

		// Verify initial heartbeat delivery to all connections
		allConnections.forEach { connection ->
			val initialHeartbeats = connection.waitForHeartbeats(HEARTBEAT_TIMEOUT, 1)
			assertThat(initialHeartbeats)
				.`as`("All connections should receive initial heartbeats")
				.hasSize(1)
		}

		// When: Simulate connection death for 2 connections
		val connectionsToKill = listOf(
			allConnections[0], // Kill first connection
			allConnections[4]  // Kill fifth connection
		)

		val initialCacheSize = connectionStore.getConnectionsCount()
		simulateConnectionDeath(connectionsToKill)

		// Wait for a heartbeat system to detect and clean up stale connections
		// Multiple heartbeat cycles should occur to trigger cleanup
		Thread.sleep(Duration.ofSeconds(2).toMillis())

		// Then: Healthy connections continue receiving heartbeats
		val healthyConnections = allConnections - connectionsToKill.toSet()
		assertThat(healthyConnections).hasSize(7) // 9 - 2 = 7

		healthyConnections.forEach { connection ->
			val heartbeats = connection.waitForHeartbeats(MULTI_HEARTBEAT_TIMEOUT, 2)
			assertThat(heartbeats)
				.`as`("Healthy connections should continue receiving heartbeats")
				.hasSize(2)
		}

		// And: Cache size should eventually decrease (stale connections removed)
		// Note: This is time-dependent and might require multiple heartbeat cycles
		val finalCacheSize = connectionStore.getConnectionsCount()
		assertThat(finalCacheSize)
			.`as`("Cache should have fewer connections after cleanup")
			.isLessThan(initialCacheSize)
	}

	@Test
	fun `Should handle partial connection death gracefully`() {
		// Given: 9 connections are established
		val allConnections = getAllConnections()
		assertThat(allConnections).hasSize(9)

		// When: Kill 3 connections
		val deadConnections = listOf(
			allConnections[1], // Kill second connection
			allConnections[5], // Kill sixth connection
			allConnections[8]  // Kill ninth connection
		)

		simulateConnectionDeath(deadConnections)

		// Wait for cleanup detection
		Thread.sleep(Duration.ofSeconds(2).toMillis())

		// Then: Healthy connections continue receiving heartbeats
		val healthyConnections = allConnections - deadConnections.toSet()
		assertThat(healthyConnections).hasSize(6) // 9 - 3 = 6

		healthyConnections.forEach { connection ->
			val heartbeats = connection.waitForHeartbeats(MULTI_HEARTBEAT_TIMEOUT, 2)
			assertThat(heartbeats)
				.`as`("Healthy connections should continue receiving heartbeats")
				.hasSize(2)
		}

		// Verify total healthy connections received heartbeats
		val totalHealthyConnections = healthyConnections.size
		assertThat(totalHealthyConnections)
			.`as`("Total healthy connections should be 6 (9 - 3)")
			.isEqualTo(6)
	}
}