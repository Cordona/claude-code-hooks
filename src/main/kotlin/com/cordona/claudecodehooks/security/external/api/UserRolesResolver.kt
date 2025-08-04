package com.cordona.claudecodehooks.security.external.api

import com.cordona.claudecodehooks.shared.enums.Role

fun interface UserRolesResolver {
	fun execute(): Set<Role>
}