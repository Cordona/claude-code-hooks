package com.cordona.claudecodehooks.infrastructure.messaging.external.api

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

fun interface ConnectionManager {
	fun connect(): SseEmitter
}