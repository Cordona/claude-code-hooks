package com.cordona.claudecodehooks.shared.commands

import com.cordona.claudecodehooks.shared.enums.Permission
import java.time.Instant

data class GenerateApiKeyCommand(
    val externalId: String,
    val name: String,
    val permissions: List<Permission>,
    val expiresAt: Instant? = null,
)