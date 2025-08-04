package com.cordona.claudecodehooks.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class MvcTestUtils {

	@Autowired
	private lateinit var mockMvc: MockMvc

	fun assertStatus(
		expectedStatus: HttpStatus,
		endpoint: String,
		payload: String,
		headers: Map<String, String> = emptyMap(),
	) {
		performRequest(expectedStatus, endpoint, payload, headers)
	}

	fun assertStatusAndReturn(
		expectedStatus: HttpStatus,
		endpoint: String,
		payload: String,
		headers: Map<String, String>,
	): String {
		return performRequest(expectedStatus, endpoint, payload, headers)
			.andReturn()
			.response
			.contentAsString
	}

	fun assertStatusWithAuthentication(
		expectedStatus: HttpStatus,
		endpoint: String,
		payload: String,
		authentication: Authentication,
		headers: Map<String, String> = emptyMap(),
	) {
		performRequestWithAuthentication(expectedStatus, endpoint, payload, authentication, headers)
	}

	fun assertStatusWithAuthenticationAndReturn(
		expectedStatus: HttpStatus,
		endpoint: String,
		payload: String,
		authentication: Authentication,
		headers: Map<String, String> = emptyMap(),
	): String {
		return performRequestWithAuthentication(expectedStatus, endpoint, payload, authentication, headers)
			.andReturn()
			.response
			.contentAsString
	}

	fun assertDeleteWithAuthentication(
		expectedStatus: HttpStatus,
		endpoint: String,
		authentication: Authentication,
		headers: Map<String, String> = emptyMap(),
	) {
		performDeleteWithAuthentication(expectedStatus, endpoint, authentication, headers)
	}

	private fun performRequest(
		expectedStatus: HttpStatus,
		endpoint: String,
		payload: String,
		headers: Map<String, String>,
	): org.springframework.test.web.servlet.ResultActions {
		val request = post(endpoint)
			.contentType(APPLICATION_JSON)
			.content(payload)

		headers.forEach { (name, value) ->
			request.header(name, value)
		}

		val statusMatcher = when (expectedStatus) {
			HttpStatus.OK -> status().isOk
			HttpStatus.BAD_REQUEST -> status().isBadRequest
			HttpStatus.UNAUTHORIZED -> status().isUnauthorized
			HttpStatus.FORBIDDEN -> status().isForbidden
			HttpStatus.NOT_FOUND -> status().isNotFound
			HttpStatus.INTERNAL_SERVER_ERROR -> status().isInternalServerError
			else -> status().`is`(expectedStatus.value())
		}

		return mockMvc
			.perform(request)
			.andDo(print())
			.andExpect(statusMatcher)
	}

	private fun performRequestWithAuthentication(
		expectedStatus: HttpStatus,
		endpoint: String,
		payload: String,
		authentication: Authentication,
		headers: Map<String, String>,
	): org.springframework.test.web.servlet.ResultActions {
		val request = post(endpoint)
			.contentType(APPLICATION_JSON)
			.content(payload)
			.with(SecurityMockMvcRequestPostProcessors.authentication(authentication))

		headers.forEach { (name, value) ->
			request.header(name, value)
		}

		val statusMatcher = when (expectedStatus) {
			HttpStatus.OK -> status().isOk
			HttpStatus.NO_CONTENT -> status().isNoContent
			HttpStatus.BAD_REQUEST -> status().isBadRequest
			HttpStatus.UNAUTHORIZED -> status().isUnauthorized
			HttpStatus.FORBIDDEN -> status().isForbidden
			HttpStatus.NOT_FOUND -> status().isNotFound
			HttpStatus.INTERNAL_SERVER_ERROR -> status().isInternalServerError
			else -> status().`is`(expectedStatus.value())
		}

		return mockMvc
			.perform(request)
			.andDo(print())
			.andExpect(statusMatcher)
	}

	private fun performDeleteWithAuthentication(
		expectedStatus: HttpStatus,
		endpoint: String,
		authentication: Authentication,
		headers: Map<String, String>,
	): org.springframework.test.web.servlet.ResultActions {
		val request = delete(endpoint)
			.with(SecurityMockMvcRequestPostProcessors.authentication(authentication))

		headers.forEach { (name, value) ->
			request.header(name, value)
		}

		val statusMatcher = when (expectedStatus) {
			HttpStatus.OK -> status().isOk
			HttpStatus.NO_CONTENT -> status().isNoContent
			HttpStatus.BAD_REQUEST -> status().isBadRequest
			HttpStatus.UNAUTHORIZED -> status().isUnauthorized
			HttpStatus.FORBIDDEN -> status().isForbidden
			HttpStatus.NOT_FOUND -> status().isNotFound
			HttpStatus.INTERNAL_SERVER_ERROR -> status().isInternalServerError
			else -> status().`is`(expectedStatus.value())
		}

		return mockMvc
			.perform(request)
			.andDo(print())
			.andExpect(statusMatcher)
	}
}