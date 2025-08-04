package com.cordona.claudecodehooks.modulith

import com.cordona.claudecodehooks.Application
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

	private val modules = ApplicationModules.of(Application::class.java)

	@Test
	fun `Application does not violate Modulith rules`() {
		println("Existing Modules")
		println("----------------")
		println(modules)

		modules.verify()
	}
}