package com.cordona.claudecodehooks.domain

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Service Layer",
	allowedDependencies = [
		"shared::enums",
		"shared::models", 
		"shared::exceptions",
		"shared::commands",
		"shared::utils",
		"infrastructure::api",
		"persistence::api"
	]
)
@PackageInfo
class ModuleMetadata