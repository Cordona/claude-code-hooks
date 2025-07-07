package com.cordona.claudecodehooks.utils

import org.json.JSONException
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.springframework.stereotype.Component

@Component
class JsonTestUtils {

	@Throws(JSONException::class)
	fun jsonAssertWithIgnoredFields(
		actualJson: String,
		expectedJson: String,
		vararg ignoredFields: String,
	) {
		if (ignoredFields.isEmpty()) {
			JSONAssert.assertEquals(expectedJson, actualJson, STRICT)
		} else {
			val customizations = ignoredFields.map { Customization(it) { _, _ -> true } }
			val comparator = CustomComparator(STRICT, *customizations.toTypedArray())
			JSONAssert.assertEquals(expectedJson, actualJson, comparator)
		}
	}
}