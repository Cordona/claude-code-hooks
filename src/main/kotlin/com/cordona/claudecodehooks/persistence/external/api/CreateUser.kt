package com.cordona.claudecodehooks.persistence.external.api

import com.cordona.claudecodehooks.shared.enums.Role
import com.cordona.claudecodehooks.shared.models.User

fun interface CreateUser {
	fun execute(externalId: String, roles: Set<Role>): User
}