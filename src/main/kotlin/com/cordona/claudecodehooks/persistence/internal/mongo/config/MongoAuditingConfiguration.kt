package com.cordona.claudecodehooks.persistence.internal.mongo.config

import com.cordona.claudecodehooks.shared.constants.JwtClaims.EMAIL
import com.cordona.claudecodehooks.shared.constants.JwtClaims.PREFERRED_USERNAME
import com.cordona.claudecodehooks.shared.constants.JwtClaims.SUB
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import java.util.Optional

@Configuration
@EnableMongoAuditing(auditorAwareRef = "jwtAuditorAware")
class MongoAuditingConfiguration

@Component
class JwtAuditorAware : AuditorAware<String> {

	override fun getCurrentAuditor(): Optional<String> {
		return try {
			val authentication = SecurityContextHolder.getContext().authentication

			val auditor = when (authentication) {
				is JwtAuthenticationToken -> {
					val jwt = authentication.token
					jwt.getClaimAsString(PREFERRED_USERNAME)
						?: jwt.getClaimAsString(SUB)
						?: jwt.getClaimAsString(EMAIL)
				}

				else -> authentication?.name
			} ?: DEFAULT_AUDITOR

			Optional.of(auditor)
		} catch (_: Exception) {
			Optional.of(DEFAULT_AUDITOR)
		}
	}

	companion object {
		private const val DEFAULT_AUDITOR = "system"
	}
}