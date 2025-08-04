package com.cordona.claudecodehooks.shared.exceptions

import org.springframework.http.HttpStatus

sealed class ServiceException(
    val details: ExceptionDetails,
    cause: Throwable? = null
) : RuntimeException(details.message, cause) {
    
    val statusCode: HttpStatus get() = details.statusCode
}