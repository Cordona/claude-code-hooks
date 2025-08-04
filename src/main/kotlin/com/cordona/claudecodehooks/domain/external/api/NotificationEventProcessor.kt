package com.cordona.claudecodehooks.domain.external.api

import com.cordona.claudecodehooks.shared.models.NotificationHook

fun interface NotificationEventProcessor {
	fun execute(target: NotificationHook)
}