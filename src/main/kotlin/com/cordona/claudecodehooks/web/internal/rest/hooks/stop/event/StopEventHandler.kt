package com.cordona.claudecodehooks.web.internal.rest.hooks.stop.event

import com.cordona.claudecodehooks.service.external.api.StopHookProcessor
import com.cordona.claudecodehooks.web.internal.rest.hooks.common.HttpHeaderConstants.X_CONTEXT_WORK_DIRECTORY
import com.cordona.claudecodehooks.web.internal.rest.hooks.common.extensions.bodyAs
import com.cordona.claudecodehooks.web.internal.rest.hooks.stop.request.StopHookRequest
import com.cordona.claudecodehooks.web.internal.rest.hooks.stop.request.toModel
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class StopEventHandler(
	private val objectMapper: ObjectMapper,
	private val stopHookProcessor: StopHookProcessor,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val workDirectory = request.headers().firstHeader(X_CONTEXT_WORK_DIRECTORY)
		val stopHook = request.bodyAs<StopHookRequest>(objectMapper).toModel(workDirectory)

		stopHookProcessor.execute(stopHook)

		return ServerResponse.ok()
			.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
			.body("Successfully processed Stop Hook")
	}
}