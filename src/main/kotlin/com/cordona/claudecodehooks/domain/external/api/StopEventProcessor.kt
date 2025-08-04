package com.cordona.claudecodehooks.domain.external.api

import com.cordona.claudecodehooks.shared.models.StopHook

fun interface StopEventProcessor {
	fun execute(target: StopHook)
}