package com.cordona.claudecodehooks.base

import com.cordona.claudecodehooks.config.TestSecurityConfig
import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import com.cordona.claudecodehooks.utils.MvcTestUtils
import com.cordona.claudecodehooks.utils.SseAssertions
import com.cordona.claudecodehooks.utils.SseTestClient
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import java.time.Duration

@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
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
	protected lateinit var restClientBuilder: RestClient.Builder

	protected lateinit var sseClient: SseTestClient

	protected lateinit var wireMockServer: WireMockServer

	@BeforeEach
	fun setUp() {
		wireMockServer = mockServer
		wireMockServer.resetAll()
	}

	@AfterEach
	fun tearDown() {
		wireMockServer.resetAll()
	}

	protected fun setupSseConnection() {
		sseClient = SseTestClient.create(
			serverPort = serverPort,
			sseEndpoint = endpointProperties.claudeCode.hooks.events.stream.connect,
			restClientBuilder = restClientBuilder,
			sseAssertions = sseAssertions
		)
		sseClient.setupAndStart()
	}

	protected fun tearDownSseConnection() {
		if (::sseClient.isInitialized) {
			sseClient.stopSafely()
		}
	}

	companion object {
		val DEFAULT_SSE_TIMEOUT: Duration = Duration.ofSeconds(1)

		private lateinit var mockServer: WireMockServer

		@JvmStatic
		@DynamicPropertySource
		fun configureWireMockProperties(registry: DynamicPropertyRegistry) {
			mockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
			mockServer.start()
		}
	}
}