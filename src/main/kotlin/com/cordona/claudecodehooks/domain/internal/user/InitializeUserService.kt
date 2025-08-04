package com.cordona.claudecodehooks.domain.internal.user

import com.cordona.claudecodehooks.domain.external.api.InitializeUser
import com.cordona.claudecodehooks.persistence.external.api.CreateUser
import com.cordona.claudecodehooks.shared.commands.InitializeUserCommand
import com.cordona.claudecodehooks.shared.models.User
import org.springframework.stereotype.Service

@Service
class InitializeUserService(
	private val createUser: CreateUser,
) : InitializeUser {
	override fun execute(command: InitializeUserCommand): User = createUser.execute(command.externalId, command.roles)
}