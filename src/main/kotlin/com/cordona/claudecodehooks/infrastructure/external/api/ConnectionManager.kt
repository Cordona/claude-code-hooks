package com.cordona.claudecodehooks.infrastructure.external.api

import com.cordona.claudecodehooks.shared.enums.DisconnectionResult
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface ConnectionManager {
	fun connect(userExternalId: String): SseEmitter
	fun disconnect(connectionId: String, userExternalId: String): DisconnectionResult
}