package com.cordona.claudecodehooks.web.internal.rest.hooks.notification.event

import com.cordona.claudecodehooks.domain.external.api.NotificationEventProcessor
import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import com.cordona.claudecodehooks.web.internal.extensions.bodyAs
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class NotificationEventHandler(
	private val objectMapper: ObjectMapper,
	private val userExternalIdResolver: UserExternalIdResolver,
	private val notificationEventProcessor: NotificationEventProcessor,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val userExternalId = userExternalIdResolver.execute()
		val notificationHook = request.bodyAs<NotificationHookRequest>(objectMapper).toModel(userExternalId)

		notificationEventProcessor.execute(notificationHook)

		return ServerResponse.noContent().build()
	}
}