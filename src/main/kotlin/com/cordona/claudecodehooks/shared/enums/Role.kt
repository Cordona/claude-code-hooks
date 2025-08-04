package com.cordona.claudecodehooks.shared.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Role(@JsonValue val value: String) {
	USER("user"),
	ADMIN("admin");
	
	companion object {
		@JsonCreator
		@JvmStatic
		fun fromValue(value: String): Role = 
			entries.find { it.value.equals(value, ignoreCase = true) }
				?: throw IllegalArgumentException("Unknown role: $value")
	}
}