package com.cordona.claudecodehooks.security

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Security Layer",
	allowedDependencies = [
		"shared::properties"
	]
)
@PackageInfo
class ModuleMetadata