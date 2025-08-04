package com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.caching

import com.cordona.claudecodehooks.infrastructure.internal.messaging.sse.connection.ConnectionDetails
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface ConnectionStore {
	fun addConnection(userExternalId: String, connectionDetails: ConnectionDetails, emitter: SseEmitter)
	fun removeConnection(connectionId: String): Boolean
	fun removeUserConnection(userExternalId: String, connectionId: String): Boolean
	fun getUserConnections(userExternalId: String): Set<ConnectionDetails>?
	fun getEmitter(connectionId: String): SseEmitter?
	fun getConnectionsCount(): Long
	fun getAllConnections(): List<Pair<ConnectionDetails, SseEmitter>>
	fun updateConnectionHealth(connectionId: String, updatedDetails: ConnectionDetails): Boolean
	fun getUnhealthyConnections(maxFailures: Int): List<ConnectionDetails>
}