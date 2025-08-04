package com.cordona.claudecodehooks.persistence.internal.mongo.entities

import com.cordona.claudecodehooks.shared.enums.Role
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

@Document("users")
data class UserEntity(
	@Id
	val id: String = UUID.randomUUID().toString(),

	@Indexed(unique = true)
	val externalId: String,

	val roles: Set<Role>,

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