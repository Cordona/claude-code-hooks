package com.cordona.claudecodehooks.web.internal.rest.error

import com.cordona.claudecodehooks.shared.exceptions.ServiceException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.servlet.function.ServerResponse

@Component
class ExceptionHandler {

	private val logger = LoggerFactory.getLogger(ExceptionHandler::class.java)

	fun handle(exception: Exception): ServerResponse {
		return when (exception) {
			is ServiceException -> {
				handleServiceException(exception)
			}

			else -> {
				handleGenericException(exception)
			}
		}
	}

	private fun handleServiceException(exception: ServiceException): ServerResponse {
		logger.warn("Service exception occurred: ${exception.message}", exception)

		val errorResponse = ErrorResponse(
			statusCode = exception.statusCode.value(),
			exceptionName = exception::class.simpleName!!,
			message = exception.message ?: "Service error occurred"
		)

		return ServerResponse
			.status(exception.statusCode)
			.contentType(APPLICATION_JSON)
			.body(errorResponse)
	}


	private fun handleGenericException(throwable: Throwable): ServerResponse {
		logger.error("Unexpected exception occurred: ${throwable.message}", throwable)

		val errorResponse = ErrorResponse(
			statusCode = INTERNAL_SERVER_ERROR.value(),
			exceptionName = throwable::class.simpleName!!,
			message = "Internal server error occurred"
		)

		return ServerResponse
			.status(INTERNAL_SERVER_ERROR)
			.contentType(APPLICATION_JSON)
			.body(errorResponse)
	}
}