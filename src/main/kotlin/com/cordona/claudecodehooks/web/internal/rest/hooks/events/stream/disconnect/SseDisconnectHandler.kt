package com.cordona.claudecodehooks.web.internal.rest.hooks.events.stream.disconnect

import com.cordona.claudecodehooks.infrastructure.external.api.ConnectionManager
import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import com.cordona.claudecodehooks.shared.enums.DisconnectionResult
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class SseDisconnectHandler(
	private val connectionManager: ConnectionManager,
	private val userExternalIdResolver: UserExternalIdResolver,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val userExternalId = userExternalIdResolver.execute()
		val connectionId = request.pathVariable("connectionId")

		return when (connectionManager.disconnect(connectionId, userExternalId)) {
			DisconnectionResult.SUCCESS -> ServerResponse.noContent().build()
			DisconnectionResult.NOT_FOUND -> ServerResponse.notFound().build()
			DisconnectionResult.FORBIDDEN -> ServerResponse.status(FORBIDDEN).build()
		}
	}
}