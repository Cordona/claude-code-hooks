package com.cordona.claudecodehooks.shared.exceptions

import io.github.oshai.kotlinlogging.KLogger

object ExceptionUtils {

    fun <T : ServiceException> logAndRaise(
        exceptionFactory: (ExceptionDetails, Throwable?) -> T,
        details: ExceptionDetails,
        logger: KLogger,
        cause: Throwable? = null
    ): Nothing {
        val errorMessage = "Error occurred: ${details.message}. Details: ${details.statusCode}"
        
        if (cause != null) {
            logger.error(cause) { errorMessage }
        } else {
            logger.error { errorMessage }
        }
        
        throw exceptionFactory(details, cause)
    }

    fun <T : ServiceException> logAndRaise(
        exceptionFactory: (ExceptionDetails, Throwable?) -> T,
        details: ExceptionDetails,
        logger: KLogger,
        cause: Throwable? = null,
        vararg formatArgs: Any
    ): Nothing {
        val formattedMessage = if (formatArgs.isNotEmpty()) {
            details.message.format(*formatArgs)
        } else {
            details.message
        }
        
        val errorMessage = "Error occurred: $formattedMessage. Details: ${details.statusCode}"
        
        if (cause != null) {
            logger.error(cause) { errorMessage }
        } else {
            logger.error { errorMessage }
        }
        
        val formattedDetails = details.copy(message = formattedMessage)
        throw exceptionFactory(formattedDetails, cause)
    }
}