package com.cordona.claudecodehooks.security

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Security Layer",
	allowedDependencies = [
		"shared::properties",
		"shared::models", 
		"shared::enums",
		"shared::exceptions",
		"shared::utils",
		"persistence::api"
	]
)
@PackageInfo
class ModuleMetadata