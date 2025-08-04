package com.cordona.claudecodehooks.shared.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class HookType(@JsonValue val value: String) {
	STOP("stop"),
	NOTIFICATION("notification");

	companion object {
		@JsonCreator
		@JvmStatic
		fun fromValue(value: String): HookType =
			entries.find { it.value.equals(value, ignoreCase = true) }
				?: throw IllegalArgumentException("Unknown hook type: $value")
	}
}