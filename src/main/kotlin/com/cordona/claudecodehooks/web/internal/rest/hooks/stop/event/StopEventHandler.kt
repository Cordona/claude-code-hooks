package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.event

import com.cordona.claudecodehooks.domain.external.api.StopEventProcessor
import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import com.cordona.claudecodehooks.web.internal.extensions.bodyAs
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class StopEventHandler(
	private val objectMapper: ObjectMapper,
	private val userExternalIdResolver: UserExternalIdResolver,
	private val stopEventProcessor: StopEventProcessor,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val userExternalId = userExternalIdResolver.execute()
		val stopHook = request.bodyAs<StopHookRequest>(objectMapper).toModel(userExternalId)

		stopEventProcessor.execute(stopHook)

		return ServerResponse.noContent().build()
	}
}