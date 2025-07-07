package com.cordona.claudecodehooks.web

import com.cordona.claudecodehooks.base.BaseWebTest
import org.junit.jupiter.api.Test


class NotificationHookIntegrationTest : BaseWebTest() {

	@Test
	fun `Should send notification event when bash permission requested`() {
		val requestJson = fileTestUtils.fileToString(SOURCE_DIR, "notification_hook_request_for_bash_use.json")

		mvcTestUtils.assertIsOk(
			endpoint = endpointProperties.claudeCode.hooks.notification.event,
			payload = requestJson
		)

		val hookEvents = sseClient.waitForEvents(DEFAULT_SSE_TIMEOUT, expectedCount = 1)

		sseAssertions.assertEventsCount(hookEvents, 1)

		val actual = hookEvents.first()
		val expected = fileTestUtils.fileToString(SOURCE_DIR, "expected_event_for_bash_use_notification.json")

		jsonTestUtils.jsonAssertWithIgnoredFields(actual, expected, "id", "timestamp")
	}

	@Test
	fun `Should send notification event when plan is ready`() {
		val requestJson =
			fileTestUtils.fileToString(SOURCE_DIR, "notification_hook_request_for_plan_approval.json")

		mvcTestUtils.assertIsOk(
			endpoint = endpointProperties.claudeCode.hooks.notification.event,
			payload = requestJson
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
		private const val IGNORED_FIELD = "id"
	}
}