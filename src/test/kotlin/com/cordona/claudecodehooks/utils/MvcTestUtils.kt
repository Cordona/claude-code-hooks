package com.cordona.claudecodehooks.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD
import org.springframework.http.HttpHeaders.ORIGIN
import org.springframework.http.HttpMethod.GET
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class MvcTestUtils {

	@Autowired
	private lateinit var mockMvc: MockMvc

	fun assertIsOk(
		endpoint: String,
		payload: String,
		headers: Map<String, String>,
	) {
		val request = post(endpoint)
			.contentType(APPLICATION_JSON)
			.content(payload)

		headers.forEach { (name, value) ->
			request.header(name, value)
		}

		mockMvc
			.perform(request)
			.andExpect(status().isOk)
	}


	fun assertCorsAllowed(origin: String, endpoint: String) {
		// Test OPTIONS preflight request
		mockMvc.perform(
			options(endpoint)
				.header(ORIGIN, origin)
				.header(ACCESS_CONTROL_REQUEST_METHOD, GET.name())
		)
			.andExpect(status().isOk)
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, origin))

		// Test actual GET request for SSE
		mockMvc.perform(
			get(endpoint)
				.header(ORIGIN, origin)
				.header(ACCEPT, TEXT_EVENT_STREAM.toString())
		)
			.andExpect(status().isOk)
			.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, origin))
	}

	fun assertCorsBlocked(origin: String, endpoint: String) {
		// Test OPTIONS preflight request - should be forbidden for non-allowed origins
		mockMvc.perform(
			options(endpoint)
				.header(ORIGIN, origin)
				.header(ACCESS_CONTROL_REQUEST_METHOD, GET.name())
		)
			.andExpect(status().isForbidden)
	}
}