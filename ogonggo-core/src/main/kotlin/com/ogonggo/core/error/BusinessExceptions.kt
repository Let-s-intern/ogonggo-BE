package com.ogonggo.core.error

/**
 * 메시지는 그대로 응답에 나가므로 ErrorCode의 메시지가 기본이다.
 * 클라이언트가 보낸 값 중 무엇이 문제인지 알려 줘야 할 때만 바꾸며, 내부 정보는 담지 않는다.
 */
open class BusinessException(
    val errorCode: ErrorCode,
    message: String = errorCode.message,
) : RuntimeException(message)

class InvalidValueException(errorCode: ErrorCode) : BusinessException(errorCode)

class EntityNotFoundException(errorCode: ErrorCode, message: String = errorCode.message) : BusinessException(errorCode, message)

class ConflictException(errorCode: ErrorCode) : BusinessException(errorCode)

class UnauthorizedException(errorCode: ErrorCode) : BusinessException(errorCode)

class ForbiddenException(errorCode: ErrorCode) : BusinessException(errorCode)

class InternalServerException(errorCode: ErrorCode) : BusinessException(errorCode)
