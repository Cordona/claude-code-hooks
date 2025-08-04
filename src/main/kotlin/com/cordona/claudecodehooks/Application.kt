package com.cordona.claudecodehooks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulith
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@Modulith
@EnableAsync
@EnableScheduling
@StrictModulith
@SpringBootApplication
@ConfigurationPropertiesScan
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}