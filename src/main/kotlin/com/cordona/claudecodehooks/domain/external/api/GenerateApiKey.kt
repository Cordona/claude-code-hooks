package com.cordona.claudecodehooks.domain.external.api

import com.cordona.claudecodehooks.shared.commands.GenerateApiKeyCommand
import com.cordona.claudecodehooks.shared.models.ApiKey

fun interface GenerateApiKey {
	fun execute(command: GenerateApiKeyCommand): ApiKey
}