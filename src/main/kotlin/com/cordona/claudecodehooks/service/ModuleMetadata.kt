package com.cordona.claudecodehooks.service

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Service Layer",
	allowedDependencies = [
		"shared::enums",
		"shared::models",
		"infrastructure::api"
	]
)
@PackageInfo
class ModuleMetadata