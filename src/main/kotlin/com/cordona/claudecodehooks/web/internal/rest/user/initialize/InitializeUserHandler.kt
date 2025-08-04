package com.cordona.claudecodehooks.web.internal.rest.user.initialize

import com.cordona.claudecodehooks.domain.external.api.InitializeUser
import com.cordona.claudecodehooks.security.external.api.UserExternalIdResolver
import com.cordona.claudecodehooks.security.external.api.UserRolesResolver
import com.cordona.claudecodehooks.shared.commands.InitializeUserCommand
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.HandlerFunction
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

@Component
class InitializeUserHandler(
	private val initializeUser: InitializeUser,
	private val userExternalIdResolver: UserExternalIdResolver,
	private val userRolesResolver: UserRolesResolver,
) : HandlerFunction<ServerResponse> {

	override fun handle(request: ServerRequest): ServerResponse {
		val externalId = userExternalIdResolver.execute()
		val roles = userRolesResolver.execute()

		val command = InitializeUserCommand(
			externalId = externalId,
			roles = roles
		)

		initializeUser.execute(command)

		return ServerResponse.noContent().build()
	}
}