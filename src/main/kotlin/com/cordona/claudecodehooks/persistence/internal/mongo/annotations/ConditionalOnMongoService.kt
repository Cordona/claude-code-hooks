package com.cordona.claudecodehooks.persistence.internal.mongo.annotations

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Service
@ConditionalOnProperty(
	prefix = "service.database",
	name = ["provider"],
	havingValue = "mongo"
)
annotation class ConditionalOnMongoService