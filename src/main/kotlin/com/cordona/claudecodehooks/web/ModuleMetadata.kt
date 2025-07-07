package com.cordona.claudecodehooks.web

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
	displayName = "Web Layer",
	allowedDependencies = [
		"shared::enums",
		"shared::models",
		"service::api",
		"infrastructure::api"
	]
)
@PackageInfo
class ModuleMetadata