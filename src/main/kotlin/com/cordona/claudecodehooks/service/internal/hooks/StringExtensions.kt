package com.cordona.claudecodehooks.service.internal.hooks

fun String.resolveProjectContext(): String {
    return this
        .substringAfter("/projects/")
        .substringAfter("-")
        .substringAfterLast("-IdeaProjects-")
        .substringBefore("/")
}