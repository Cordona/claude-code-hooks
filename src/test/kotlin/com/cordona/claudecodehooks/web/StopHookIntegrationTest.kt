package com.cordona.claudecodehooks.web

import com.cordona.claudecodehooks.base.BaseWebTest
import org.junit.jupiter.api.Test

class StopHookIntegrationTest : BaseWebTest() {

	@Test
	fun `Should send stop event when hook is deactivated`() {
		val requestJson = fileTestUtils.fileToString(SOURCE_DIR, "stop_hook_request.json")

		mvcTestUtils.assertIsOk(
			endpoint = endpointProperties.claudeCode.hooks.stop.event,
			payload = requestJson,
			headers = mapOf("X-Context-Work-Directory" to "people-spheres-claude-code-custom-commands")
		)

		val hookEvents = sseClient.waitForEvents(DEFAULT_SSE_TIMEOUT, expectedCount = 1)

		sseAssertions.assertEventsCount(hookEvents, 1)

		val actual = hookEvents.first()
		val expected = fileTestUtils.fileToString(SOURCE_DIR, "expected_event_for_stop_hook.json")

		jsonTestUtils.jsonAssertWithIgnoredFields(actual, expected, "id", "timestamp")
	}

	companion object {
		private const val SOURCE_DIR = "hooks/stop"
		private const val IGNORED_FIELD = "id"
	}
}