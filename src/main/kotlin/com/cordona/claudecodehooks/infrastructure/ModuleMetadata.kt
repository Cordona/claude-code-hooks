package com.cordona.claudecodehooks.infrastructure

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Infrastructure Layer",
	allowedDependencies = [
		"shared::enums",
		"shared::models"
	]
)
@PackageInfo
class ModuleMetadata