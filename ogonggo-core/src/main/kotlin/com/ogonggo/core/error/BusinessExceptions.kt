package com.ogonggo.core.error

open class BusinessException(
    val errorCode: ErrorCode,
    message: String = errorCode.message,
) : RuntimeException(message)

class InvalidValueException(errorCode: ErrorCode) : BusinessException(errorCode)

class EntityNotFoundException(errorCode: ErrorCode) : BusinessException(errorCode)

class ConflictException(errorCode: ErrorCode) : BusinessException(errorCode)

class UnauthorizedException(errorCode: ErrorCode) : BusinessException(errorCode)

class ForbiddenException(errorCode: ErrorCode) : BusinessException(errorCode)

class InternalServerException(errorCode: ErrorCode) : BusinessException(errorCode)
