package com.cordona.claudecodehooks.persistence.external.api

import com.cordona.claudecodehooks.shared.models.ApiKey

fun interface FindApiKeyByLookupHash {
	fun execute(lookupHash: String): ApiKey?
}