package com.cordona.claudecodehooks.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class MvcTestUtils {

	@Autowired
	private lateinit var mockMvc: MockMvc

	fun assertIsOk(
		endpoint: String,
		payload: String,
	) {
		val request = post(endpoint)
			.contentType(APPLICATION_JSON)
			.content(payload)

		mockMvc
			.perform(request)
			.andExpect(status().isOk)
	}
}