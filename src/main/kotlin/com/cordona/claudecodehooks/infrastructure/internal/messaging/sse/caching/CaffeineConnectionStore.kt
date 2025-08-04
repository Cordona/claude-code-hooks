package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching

import com.cordona.claudecodehooks.infrastructure.internal.annotation.ConditionalOnCaffeineCache
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection.ConnectionDetails
import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.properties.SseProperties
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnCaffeineCache
class CaffeineConnectionStore(
	private val sseProperties: SseProperties,
) : ConnectionStore {

	private val logger = KotlinLogging.logger {}

	private val connectionsCache: Cache<String, MutableSet<ConnectionDetails>> = createConnectionsCache()
	private val emittersCache: Cache<String, SseEmitter> = createEmittersCache()

	override fun addConnection(userExternalId: String, connectionDetails: ConnectionDetails, emitter: SseEmitter) {
		val connections = connectionsCache[userExternalId, { ConcurrentHashMap.newKeySet() }]
		connections.add(connectionDetails)
		emittersCache.put(connectionDetails.connectionId, emitter)
		logger.debug { "Added connection with ID ${connectionDetails.connectionId} for user with external ID $userExternalId" }
	}

	override fun removeConnection(connectionId: String): Boolean {
		val emitter = emittersCache.getIfPresent(connectionId)
		if (emitter != null) {
			emittersCache.invalidate(connectionId)
		}

		var removed = false

		connectionsCache.asMap().forEach { (userExternalId, connections) ->
			val connectionRemoved = connections.removeIf { it.connectionId == connectionId }

			if (connectionRemoved) {
				removed = true
				if (connections.isEmpty()) {
					connectionsCache.invalidate(userExternalId)
					logger.debug { "Removed connection with ID $connectionId and invalidated empty user cache for user with external ID $userExternalId" }
				} else {
					logger.debug { "Removed connection with ID $connectionId for user with external ID $userExternalId" }
				}
			}
		}

		return removed
	}

	override fun removeUserConnection(userExternalId: String, connectionId: String): Boolean {
		var removed = false

		connectionsCache.getIfPresent(userExternalId)?.let { connections ->
			removed = connections.removeIf { it.connectionId == connectionId }

			if (removed) {
				emittersCache.invalidate(connectionId)

				if (connections.isEmpty()) {
					connectionsCache.invalidate(userExternalId)
					logger.debug { "Removed last connection for user with external ID $userExternalId, invalidated user cache entry" }
				} else {
					logger.debug { "Removed connection with ID $connectionId for user with external ID $userExternalId" }
				}
			}
		}

		return removed
	}

	override fun updateConnectionHealth(connectionId: String, updatedDetails: ConnectionDetails): Boolean {
		var updated = false

		connectionsCache.asMap().forEach { (_, connections) ->
			val connectionToUpdate = connections.find { it.connectionId == connectionId }
			if (connectionToUpdate != null) {
				connections.remove(connectionToUpdate)
				connections.add(updatedDetails)
				updated = true
				logger.trace { "Updated health for connection with ID $connectionId" }
			}
		}

		return updated
	}

	override fun getUserConnections(userExternalId: String): Set<ConnectionDetails>? = connectionsCache.getIfPresent(userExternalId)

	override fun getEmitter(connectionId: String): SseEmitter? = emittersCache.getIfPresent(connectionId)

	override fun getConnectionsCount(): Long = emittersCache.estimatedSize()

	override fun getAllConnections(): List<Pair<ConnectionDetails, SseEmitter>> {
		return connectionsCache.asMap().values
			.flatMap { connections -> connections.toSet() }
			.mapNotNull { metadata ->
				emittersCache.getIfPresent(metadata.connectionId)?.let { emitter ->
					metadata to emitter
				}
			}
	}

	override fun getUnhealthyConnections(maxFailures: Int): List<ConnectionDetails> {
		return connectionsCache.asMap().values
			.flatMap { connections -> connections.toSet() }
			.filter { it.isUnhealthy(maxFailures) }
	}

	private fun createConnectionsCache(): Cache<String, MutableSet<ConnectionDetails>> {
		return Caffeine.newBuilder()
			.maximumSize(sseProperties.cache.maxUsers)
			.recordStats()
			.removalListener { userExternalId: String?, connections: MutableSet<ConnectionDetails>?, cause ->
				logger.debug { "Cache entry removed for user with external ID $userExternalId, connections: ${connections?.size}, cause: $cause" }
			}
			.build<String, MutableSet<ConnectionDetails>>()
	}

	private fun createEmittersCache(): Cache<String, SseEmitter> {
		return Caffeine.newBuilder()
			.maximumSize(sseProperties.cache.maxConnections)
			.recordStats()
			.removalListener { connectionId: String?, emitter: SseEmitter?, cause ->
				logger.debug { "Emitter cache entry removed: $connectionId, cause: $cause" }
				runCatching { emitter?.complete() }
			}
			.build<String, SseEmitter>()
	}
}