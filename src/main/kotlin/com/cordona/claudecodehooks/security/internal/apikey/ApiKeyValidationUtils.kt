package com.cordona.claudecodehooks.security.internal.apikey

import com.cordona.claudecodehooks.security.internal.apikey.models.ApiKeyAuthenticationToken
import com.cordona.claudecodehooks.shared.enums.Permission.HOOKS_WRITE
import com.cordona.claudecodehooks.shared.properties.EndpointProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class ApiKeyValidationUtils(
	private val endpointProperties: EndpointProperties,
) {

	fun doesNotHaveRequiredPermissions(request: HttpServletRequest, token: ApiKeyAuthenticationToken): Boolean {
		return when (request.requestURI) {
			endpointProperties.claudeCode.hooks.notification.event ->
				token.doesNotHavePermission(HOOKS_WRITE)

			endpointProperties.claudeCode.hooks.stop.event ->
				token.doesNotHavePermission(HOOKS_WRITE)

			else -> false
		}
	}
}