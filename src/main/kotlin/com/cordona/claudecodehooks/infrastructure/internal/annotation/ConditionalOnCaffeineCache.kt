package com.cordona.claudecodehooks.infrastructure.internal.annotation

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Target(CLASS)
@Retention(RUNTIME)
@Service
@ConditionalOnProperty(
	prefix = "service.cache",
	name = ["store"],
	havingValue = "caffeine"
)
annotation class ConditionalOnCaffeineCache