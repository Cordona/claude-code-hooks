package com.cordona.claudecodehooks.service.external.api

import com.cordona.claudecodehooks.shared.models.NotificationHook

fun interface NotificationHookProcessor {
	fun execute(target: NotificationHook)
}