package com.cordona.claudecodehooks.web.integration

import com.cordona.claudecodehooks.base.BaseWebTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NO_CONTENT


class NotificationHookTest : BaseWebTest() {

	@BeforeEach
	fun setUpSseConnection() {
		setupSseConnection()
	}

	@AfterEach
	fun tearDownSse() {
		tearDownSseConnection()
	}

	@Test
	fun `Should send notification event when bash permission requested`() {
		val payload = fileTestUtils.fileToString(SOURCE_DIR, "notification_hook_request_for_bash_use.json")

		mvcTestUtils.assertStatus(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.hooks.notification.event,
			payload = payload
		)

		val hookEvents = sseClient.waitForEvents(DEFAULT_SSE_TIMEOUT, expectedCount = 1)

		sseAssertions.assertEventsCount(hookEvents, 1)

		val actual = hookEvents.first()
		val expected = fileTestUtils.fileToString(SOURCE_DIR, "expected_event_for_bash_use_notification.json")

		jsonTestUtils.jsonAssertWithIgnoredFields(actual, expected, "id", "timestamp")
	}

	@Test
	fun `Should send notification event when plan is ready`() {
		val payload = fileTestUtils.fileToString(SOURCE_DIR, "notification_hook_request_for_plan_approval.json")

		mvcTestUtils.assertStatus(
			expectedStatus = NO_CONTENT,
			endpoint = endpointProperties.claudeCode.hooks.notification.event,
			payload = payload
		)

		val hookEvents = sseClient.waitForEvents(DEFAULT_SSE_TIMEOUT, expectedCount = 1)

		sseAssertions.assertEventsCount(hookEvents, 1)

		val actual = hookEvents.first()
		val expected =
			fileTestUtils.fileToString(SOURCE_DIR, "expected_event_for_plan_approval.json.json")

		jsonTestUtils.jsonAssertWithIgnoredFields(actual, expected, "id", "timestamp")
	}

	companion object {
		private const val SOURCE_DIR = "hooks/notification"
	}
}