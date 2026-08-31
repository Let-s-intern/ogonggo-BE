package com.ogonggo.core.error

import org.springframework.http.HttpStatus

interface ErrorCode {
    val httpStatus: HttpStatus
    val message: String

    val code: String
        get() = (this as? Enum<*>)?.name ?: "UNKNOWN"
}
