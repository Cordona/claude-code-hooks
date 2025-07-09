package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.event

import com.cordona.claudecodehooks.service.external.api.NotificationHookProcessor
import com.cordona.claudecodehooks.web.internal.rest.hooks.common.HttpHeaderConstants.X_CONTEXT_WORK_DIRECTORY
import com.cordona.claudecodehooks.web.internal.rest.hooks.common.extensions.bodyAs
import com.cordona.claudecodehooks.web.internal.rest.hooks.notification.request.NotificationHookRequest
import com.cordona.claudecodehooks.web.internal.rest.hooks.notification.request.toModel
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class NotificationEventHandler(
	private val objectMapper: ObjectMapper,
	private val notificationHookProcessor: NotificationHookProcessor,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val workDirectory = request.headers().firstHeader(X_CONTEXT_WORK_DIRECTORY)
		val notificationHook = request.bodyAs<NotificationHookRequest>(objectMapper).toModel(workDirectory)

		notificationHookProcessor.execute(notificationHook)

		return ServerResponse
			.ok()
			.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
			.body("Successfully processed Notification Hook")
	}
}