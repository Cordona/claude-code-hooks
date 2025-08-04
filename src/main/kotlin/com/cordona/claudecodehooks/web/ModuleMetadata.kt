package com.cordona.claudecodehooks.web

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Web Layer",
	allowedDependencies = [
		"shared::enums",
		"shared::models",
		"shared::properties",
		"shared::exceptions",
		"shared::commands",
		"domain::api",
		"infrastructure::api",
		"security::api"
	]
)
@PackageInfo
class ModuleMetadata