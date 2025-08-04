package com.cordona.claudecodehooks.shared.commands

import com.cordona.claudecodehooks.shared.enums.Role

data class InitializeUserCommand(
    val externalId: String,
    val roles: Set<Role>,
)