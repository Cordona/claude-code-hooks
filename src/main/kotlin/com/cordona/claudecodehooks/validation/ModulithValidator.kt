package com.cordona.claudecodehooks.validation

import com.cordona.claudecodehooks.Application
import com.cordona.claudecodehooks.StrictModulith
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.modulith.core.ApplicationModules
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(prefix = "service.modulith.strict", name = ["enabled"], havingValue = "true")
class ModulithValidator(
	private val context: ConfigurableApplicationContext,
) : ApplicationListener<ContextRefreshedEvent> {
	override fun onApplicationEvent(event: ContextRefreshedEvent) {
		val isStrctModulithService = Application::class.java.isAnnotationPresent(StrictModulith::class.java)

		if (isStrctModulithService) {
			runCatching {
				ApplicationModules.of(Application::class.java).verify()
			}.onSuccess {
				logger.info { "Modularity validation passed. Service respects Modulith rules" }
			}.onFailure { exception ->
				logger.error(exception) { "Modularity validation failed. Service violates Modulith rules" }
				SpringApplication.exit(context, { 1 })
			}
		}
	}
}