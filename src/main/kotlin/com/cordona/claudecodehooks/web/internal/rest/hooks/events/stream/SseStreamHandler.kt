package com.cordona.claudecodehooks.web.internal.rest.hooks.events.stream

import com.cordona.claudecodehooks.infrastructure.messaging.external.api.ConnectionManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping
class SseStreamController(
	private val connectionManager: ConnectionManager
) {

	private val logger = KotlinLogging.logger {}

	@GetMapping(value = ["\${service.web.endpoints.claude-code.hooks.events.stream}"], produces = [TEXT_EVENT_STREAM_VALUE])
	fun streamEvents(): SseEmitter {
		val emitter = connectionManager.connect()
		logger.debug { "SSE connection established" }
		return emitter
	}
}