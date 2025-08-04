package com.cordona.claudecodehooks.domain.external.api

import com.cordona.claudecodehooks.shared.commands.InitializeUserCommand
import com.cordona.claudecodehooks.shared.models.User

fun interface InitializeUser {
	fun execute(command: InitializeUserCommand): User
}