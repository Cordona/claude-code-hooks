package com.cordona.claudecodehooks.persistence

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Persistence Layer",
	allowedDependencies = [
		"shared::models",
		"shared::exceptions",
		"shared::enums"
	]
)
@PackageInfo
class ModuleMetadata