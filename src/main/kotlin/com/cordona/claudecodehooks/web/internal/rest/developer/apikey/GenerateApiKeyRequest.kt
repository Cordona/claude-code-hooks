package com.cordona.claudecodehooks.web.internal.rest.developer.apikey

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class GenerateApiKeyRequest(
	@field:NotBlank(message = "Name is required and cannot be blank")
	val name: String,
	
	@field:Size(min = 1, message = "At least one permission is required")
	val permissions: List<String>,
	
	@JsonProperty("expiresAt")
	val expiresAt: String? = null
)