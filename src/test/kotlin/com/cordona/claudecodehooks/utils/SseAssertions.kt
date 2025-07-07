package com.cordona.claudecodehooks.utils

import org.assertj.core.api.Assertions.assertThat
import org.springframework.stereotype.Component

@Component
class SseAssertions {

	fun assertConnectionEstablished(sseClient: SseTestClient) {
		assertThat(sseClient.waitForConnection())
			.`as`("SSE connection should be established")
			.isTrue()
	}

	fun assertEventsCount(events: List<String>, expectedCount: Int) {
		assertThat(events)
			.`as`("Should receive exactly $expectedCount claude-hook event(s)")
			.hasSize(expectedCount)
	}
}