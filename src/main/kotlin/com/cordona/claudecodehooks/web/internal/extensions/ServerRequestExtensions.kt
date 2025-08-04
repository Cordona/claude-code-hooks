package com.cordona.claudecodehooks.web.internal.extensions

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.servlet.function.ServerRequest


inline fun <reified T> ServerRequest.bodyAs(objectMapper: ObjectMapper): T =
	objectMapper.readValue(body(String::class.java), object : TypeReference<T>() {})