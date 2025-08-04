package com.cordona.claudecodehooks.web.internal.rest.developer.apikey

import com.cordona.claudecodehooks.domain.external.api.GenerateApiKey
import com.cordona.claudecodehooks.shared.commands.GenerateApiKeyCommand
import com.cordona.claudecodehooks.shared.enums.Permission
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import java.time.Instant

@Component
class GenerateApiKeyHandler(
	private val generateApiKey: GenerateApiKey,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val authentication = request.principal().get() as JwtAuthenticationToken
		val externalId = authentication.token.subject
		val requestBody = request.body(GenerateApiKeyRequest::class.java)
		val permissions = requestBody.permissions.mapNotNull { Permission.fromValue(it) }
		val expiresAt = requestBody.expiresAt?.let { Instant.parse(it) }

		val command = GenerateApiKeyCommand(
			externalId = externalId,
			name = requestBody.name,
			permissions = permissions,
			expiresAt = expiresAt
		)

		val apiKey = generateApiKey.execute(command)
		val response = GenerateApiKeyResponse.from(apiKey)

		return ServerResponse.ok().body(response)
	}
}