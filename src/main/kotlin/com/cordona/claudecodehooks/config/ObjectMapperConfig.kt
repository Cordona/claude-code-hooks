package com.cordona.claudecodehooks.config

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class ObjectMapperConfig {

	@Bean
	@Primary
	fun objectMapper(): ObjectMapper {
		return ObjectMapper().apply {
			registerKotlinModule()
			registerModule(JavaTimeModule())
			configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
		}
	}
}