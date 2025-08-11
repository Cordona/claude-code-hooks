package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseWebTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import org.springframework.http.HttpStatus.NO_CONTENT

class StopHookTest : BaseWebTest() {

	@BeforeEach
	fun setUpSseConnection() {
		setupSseConnection()
	}

	@AfterEach
	fun tearDownSse() {
		tearDownSseConnection()
	}

	@Test
	fun `Should send stop event when hook is deactivated`() {
		val payload = fileTestUtils.fileToString(SOURCE_DIR, "stop_hook_request.json")

		mvcTestUtils.assertStatus(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.hooks.stop.event,
			payload = payload
		)

		val hookEvents = sseClient.waitForEvents(DEFAULT_SSE_TIMEOUT, expectedCount = 1)

		sseAssertions.assertEventsCount(hookEvents, 1)

		val actual = hookEvents.first()
		val expected = fileTestUtils.fileToString(SOURCE_DIR, "expected_event_for_stop_hook.json")

		JSONAssert.assertEquals(expected, actual, STRICT)
	}

	companion object {
		private const val SOURCE_DIR = "hooks/stop"
	}
}