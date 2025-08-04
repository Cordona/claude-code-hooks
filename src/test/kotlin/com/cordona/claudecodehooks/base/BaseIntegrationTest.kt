package com.cordona.claudecodehooks.base

import com.cordona.claudecodehooks.Application
import com.cordona.claudecodehooks.utils.FileTestUtils
import com.cordona.claudecodehooks.utils.JsonTestUtils
import com.cordona.claudecodehooks.utils.SerialTestUtils
import com.cordona.claudecodehooks.utils.TestContainers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
	classes = [Application::class],
	webEnvironment = RANDOM_PORT
)
@Testcontainers
@DirtiesContext(classMode = AFTER_CLASS)
abstract class BaseIntegrationTest : BaseTest() {

	@Autowired
	protected lateinit var fileTestUtils: FileTestUtils

	@Autowired
	protected lateinit var jsonTestUtils: JsonTestUtils

	@Autowired
	protected lateinit var serialTestUtils: SerialTestUtils

	companion object {
		@Container
		@JvmStatic
		protected val mongoContainer: MongoDBContainer = TestContainers.mongoContainer()

		@DynamicPropertySource
		@JvmStatic
		fun overrideMongoProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.data.mongodb.uri") { mongoContainer.replicaSetUrl }
			registry.add("spring.data.mongodb.database") { TestContainers.TEST_DATABASE_NAME }
		}
	}
}