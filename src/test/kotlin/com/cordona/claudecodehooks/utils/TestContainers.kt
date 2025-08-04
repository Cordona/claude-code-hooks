package com.cordona.claudecodehooks.utils

import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

object TestContainers {

    const val TEST_DATABASE_NAME = "claude_hooks_test"
    const val MONGO_PORT = 27017

    fun mongoContainer(): MongoDBContainer {
        return MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(MONGO_PORT)
            .withReuse(true)
    }
}