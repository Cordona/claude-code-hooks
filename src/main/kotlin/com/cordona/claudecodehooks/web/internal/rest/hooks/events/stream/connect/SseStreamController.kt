package com.cordona.claudecodehooks.web.internal.rest.hooks.events.stream.connect

import com.cordona.claudecodehooks.infrastructure.external.api.ConnectionManager
import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping
class SseStreamController(
	private val connectionManager: ConnectionManager,
	private val userExternalIdResolver: UserExternalIdResolver,
) {

	@GetMapping(
		value = ["\${service.web.endpoints.claude-code.hooks.events.stream.connect}"],
		produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
	)
	fun streamEvents(): SseEmitter {
		val userExternalId = userExternalIdResolver.execute()
		return connectionManager.connect(userExternalId)
	}
}