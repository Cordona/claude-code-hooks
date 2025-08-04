package com.cordona.claudecodehooks.persistence.external.api

fun interface FindUserId {
	fun execute(externalId: String): String?
}