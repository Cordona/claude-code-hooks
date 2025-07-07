package com.cordona.claudecodehooks.service.external.api

import com.cordona.claudecodehooks.shared.models.StopHook

fun interface StopHookProcessor {
	fun execute(target: StopHook)
}