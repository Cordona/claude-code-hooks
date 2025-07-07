package com.cordona.claudecodehooks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulith
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@Modulith
@StrictModulith
@ConfigurationPropertiesScan
@SpringBootApplication
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}