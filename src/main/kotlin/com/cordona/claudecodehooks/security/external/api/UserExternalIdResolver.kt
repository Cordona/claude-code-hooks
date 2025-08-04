package com.cordona.claudecodehooks.security.external.api

fun interface UserExternalIdResolver {
	fun execute(): String
}