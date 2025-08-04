package com.cordona.claudecodehooks.shared.exceptions

import org.springframework.http.HttpStatus

data class ExceptionDetails(
    val statusCode: HttpStatus,
    val message: String
)