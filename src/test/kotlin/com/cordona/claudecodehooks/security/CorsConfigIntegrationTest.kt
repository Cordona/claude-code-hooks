package com.cordona.claudecodehooks.security

import com.cordona.claudecodehooks.base.BaseWebTest
import org.junit.jupiter.api.Test

class CorsConfigIntegrationTest : BaseWebTest() {

	@Test
	fun `Should allow SSE connection from configured origin port 3000`() {
		mvcTestUtils.assertCorsAllowed(
			origin = "http://localhost:3000",
			endpoint = endpointProperties.claudeCode.hooks.events.stream
		)
	}

	@Test
	fun `Should block SSE connection from non-configured origin port 5000`() {
		mvcTestUtils.assertCorsBlocked(
			origin = "http://localhost:5000",
			endpoint = endpointProperties.claudeCode.hooks.events.stream
		)
	}
}