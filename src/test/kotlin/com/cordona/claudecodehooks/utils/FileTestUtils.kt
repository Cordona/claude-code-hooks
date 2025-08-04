package com.cordona.claudecodehooks.utils

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class FileTestUtils {

	fun fileToString(directory: String, fileName: String): String {
		val path = "assets/$directory/$fileName"
		val resource = ClassPathResource(path)
		return resource.inputStream.use { it.bufferedReader().readText() }
	}
}