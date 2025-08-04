package com.cordona.claudecodehooks.web.internal.rest.error

data class ErrorResponse(
    val statusCode: Int,
    val exceptionName: String,
    val message: String,
    val timestamp: String = java.time.Instant.now().toString()
)