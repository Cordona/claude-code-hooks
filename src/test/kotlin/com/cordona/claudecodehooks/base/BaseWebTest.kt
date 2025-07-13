package com.cordona.claudecodehooks.base

import com.cordona.claudecodehooks.utils.MvcTestUtils
import com.cordona.claudecodehooks.utils.SseAssertions
import com.cordona.claudecodehooks.utils.SseTestClient
import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient
import java.time.Duration

@AutoConfigureMockMvc
abstract class BaseWebTest : BaseIntegrationTest() {

	@LocalServerPort
	protected var serverPort: Int = 0

	@Autowired
	protected lateinit var endpointProperties: EndpointProperties

	@Autowired
	protected lateinit var mvcTestUtils: MvcTestUtils

	@Autowired
	protected lateinit var sseAssertions: SseAssertions

	@Autowired
	private lateinit var restClientBuilder: RestClient.Builder

	protected lateinit var sseClient: SseTestClient

	@BeforeEach
	fun setUp() {
		sseClient = createSseTestClient()
		sseClient.start()
		sseAssertions.assertConnectionEstablished(sseClient)
	}

	@AfterEach
	fun tearDown() {
		sseClient.stop()
	}

	private fun createSseTestClient(): SseTestClient {
		val baseUrl = "${SseTestClient.LOCALHOST_BASE_URL}:$serverPort"
		val restClient = restClientBuilder.baseUrl(baseUrl).build()
		val sseEndpoint = endpointProperties.claudeCode.hooks.events.stream

		return SseTestClient(
			restClient = restClient,
			sseEndpoint = sseEndpoint
		)
	}

	companion object {
		val DEFAULT_SSE_TIMEOUT: Duration = Duration.ofSeconds(1)
	}
}