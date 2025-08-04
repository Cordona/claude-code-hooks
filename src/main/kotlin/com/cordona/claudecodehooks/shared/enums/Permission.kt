package com.cordona.claudecodehooks.shared.enums

enum class Permission(val value: String) {
	HOOKS_WRITE("hooks:write"),
	HOOKS_READ("hooks:read"),
	ADMIN_ALL("admin:all");

	override fun toString(): String = value

	companion object {
		fun fromValue(value: String): Permission? = entries.find { it.value == value }
	}
}