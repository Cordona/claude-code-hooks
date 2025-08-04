package com.cordona.claudecodehooks.shared.exceptions

class DataAccessException(
    details: ExceptionDetails,
    cause: Throwable? = null
) : ServiceException(details, cause)