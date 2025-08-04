package com.cordona.claudecodehooks.shared.models

import com.cordona.claudecodehooks.shared.enums.Role

data class User(
	val id: String,
	val externalId: String,
	val roles: Set<Role>,
)