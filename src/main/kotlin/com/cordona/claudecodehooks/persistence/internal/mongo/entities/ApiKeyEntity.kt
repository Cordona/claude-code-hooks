package com.cordona.claudecodehooks.persistence.internal.mongo.entities

import com.cordona.claudecodehooks.shared.enums.Permission
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document("api_keys")
data class ApiKeyEntity(
	@Id
	val id: String = UUID.randomUUID().toString(),

	@Indexed
	val userId: String,

	@Indexed
	val userExternalId: String,

	@Indexed(unique = true)
	val lookupHash: String,

	val name: String? = null,

	val isActive: Boolean = true,

	val keyPrefix: String,

	val permissions: List<Permission> = listOf(Permission.HOOKS_WRITE),

	val lastUsedAt: Instant? = null,

	val expiresAt: Instant? = null,

	@CreatedDate
	var createdAt: Instant? = null,

	@LastModifiedDate
	var modifiedAt: Instant? = null,

	@CreatedBy
	var createdBy: String? = null,

	@LastModifiedBy
	var modifiedBy: String? = null,

	@Version
	var version: Long? = null,
)