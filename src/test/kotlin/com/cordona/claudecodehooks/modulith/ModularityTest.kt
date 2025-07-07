package com.cordona.claudecodehooks.modulith

import com.cordona.claudecodehooks.Application
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

    private val modules = ApplicationModules.of(Application::class.java)

    @Test
    @DisplayName("Application does not violate Modulith rules")
    fun verifiesModularStructure() {
        println("Existing Modules")
        println("----------------")
        println(modules)

        modules.verify()
    }
}