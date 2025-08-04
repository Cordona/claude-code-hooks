package com.cordona.claudecodehooks.persistence.external.api

import java.time.Instant

fun interface UpdateApiKeyLastUsed {
	fun execute(keyId: String, lastUsedAt: Instant)
}