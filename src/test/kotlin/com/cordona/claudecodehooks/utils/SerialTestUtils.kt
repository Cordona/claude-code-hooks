package com.cordona.claudecodehooks.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SerialTestUtils(@Autowired private val objectMapper: ObjectMapper) {

	fun <T> deserialize(jsonString: String, expectedType: Class<T>): T {
		return objectMapper.readValue(jsonString, expectedType)
	}
}