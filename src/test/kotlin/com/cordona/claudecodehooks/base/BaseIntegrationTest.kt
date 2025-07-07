package com.cordona.claudecodehooks.base

import com.cordona.claudecodehooks.Application
import com.cordona.claudecodehooks.utils.FileTestUtils
import com.cordona.claudecodehooks.utils.JsonTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(
	classes = [Application::class],
	webEnvironment = RANDOM_PORT
)
abstract class BaseIntegrationTest : BaseTest() {

	@Autowired
	protected lateinit var fileTestUtils: FileTestUtils

	@Autowired
	protected lateinit var jsonTestUtils: JsonTestUtils
}