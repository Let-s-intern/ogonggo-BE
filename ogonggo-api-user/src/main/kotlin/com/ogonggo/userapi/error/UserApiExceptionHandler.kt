package com.ogonggo.userapi.error

import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.editor.lexical.LexicalEditorStateException
import com.ogonggo.core.image.error.ImageUploadErrorCode
import com.ogonggo.userapi.response.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class UserApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        log.error("handle: MethodArgumentNotValidException", exception)
        return badRequest(validationMessage(exception.bindingResult.fieldErrors.map { it.field to it.defaultMessage }))
    }

    @ExceptionHandler(BindException::class)
    fun handleBind(exception: BindException): ResponseEntity<ErrorResponse> {
        log.error("handle: BindException", exception)
        return badRequest(validationMessage(exception.bindingResult.fieldErrors.map { it.field to it.defaultMessage }))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidation(exception: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
        log.error("handle: HandlerMethodValidationException", exception)
        val errors = exception.allValidationResults.flatMap { result ->
            val parameterName = result.methodParameter.parameterName ?: "parameter"
            result.resolvableErrors.map { parameterName to it.defaultMessage }
        }
        return badRequest(validationMessage(errors))
    }

    @ExceptionHandler(InvalidRequestParameterException::class)
    fun handleInvalidRequestParameter(
        exception: InvalidRequestParameterException,
    ): ResponseEntity<ErrorResponse> {
        log.error("handle: InvalidRequestParameterException", exception)
        return badRequest(validationMessage(listOf(exception.parameterName to exception.reason)))
    }

    @ExceptionHandler(InvalidRequestFieldException::class)
    fun handleInvalidRequestField(exception: InvalidRequestFieldException): ResponseEntity<ErrorResponse> {
        log.error("handle: InvalidRequestFieldException", exception)
        return badRequest(validationMessage(listOf(exception.fieldName to exception.reason)))
    }

    @ExceptionHandler(LexicalEditorStateException::class)
    fun handleInvalidLexicalEditorState(
        exception: LexicalEditorStateException,
    ): ResponseEntity<ErrorResponse> {
        log.error("handle: LexicalEditorStateException", exception)
        return badRequest(validationMessage(listOf("content" to exception.reason)))
    }

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingServletRequestPart(
        exception: MissingServletRequestPartException,
    ): ResponseEntity<ErrorResponse> {
        log.error("handle: MissingServletRequestPartException", exception)
        return if (exception.requestPartName == "file") {
            ErrorResponse.from(ImageUploadErrorCode.IMAGE_FILE_REQUIRED).toResponseEntity()
        } else {
            ErrorResponse.from(UserApiErrorCode.BAD_REQUEST).toResponseEntity()
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(exception: MaxUploadSizeExceededException): ResponseEntity<ErrorResponse> {
        log.error("handle: MaxUploadSizeExceededException", exception)
        return ErrorResponse.from(ImageUploadErrorCode.IMAGE_FILE_TOO_LARGE).toResponseEntity()
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        log.error("handle: ConstraintViolationException", exception)
        val errors = exception.constraintViolations.map { it.propertyPath.toString() to it.message }
        return badRequest(validationMessage(errors))
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
    )
    fun handleBadRequest(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("handle: BadRequest", exception)
        return ErrorResponse.from(UserApiErrorCode.BAD_REQUEST).toResponseEntity()
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(exception: BusinessException): ResponseEntity<ErrorResponse> {
        log.error("handle: BusinessException", exception)
        return ErrorResponse.from(exception.errorCode).toResponseEntity()
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        log.error("handle: HttpRequestMethodNotSupportedException", exception)
        return ErrorResponse.from(UserApiErrorCode.METHOD_NOT_ALLOWED).toResponseEntity()
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(exception: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        log.error("handle: NoResourceFoundException", exception)
        return ErrorResponse.from(UserApiErrorCode.API_NOT_FOUND).toResponseEntity()
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(
        exception: ObjectOptimisticLockingFailureException,
    ): ResponseEntity<ErrorResponse> {
        log.error("handle: ObjectOptimisticLockingFailureException", exception)
        return ErrorResponse.from(UserApiErrorCode.OPTIMISTIC_LOCK_CONFLICT).toResponseEntity()
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("handle: Exception", exception)
        return ErrorResponse.from(UserApiErrorCode.INTERNAL_SERVER_ERROR).toResponseEntity()
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        ErrorResponse.from(UserApiErrorCode.BAD_REQUEST, message).toResponseEntity()

    private fun validationMessage(errors: List<Pair<String, String?>>): String =
        errors
            .sortedWith(compareBy<Pair<String, String?>> { it.first }.thenBy { it.second.orEmpty() })
            .joinToString(", ") { (field, message) ->
                "[$field] ${message ?: UserApiErrorCode.BAD_REQUEST.message}"
            }
            .ifBlank { UserApiErrorCode.BAD_REQUEST.message }

    companion object {
        private val log = LoggerFactory.getLogger(UserApiExceptionHandler::class.java)
    }
}

/**
 * Bean Validation으로 표현할 수 없는 Query Parameter 검증 실패를 전달한다.
 * 두 파라미터의 관계처럼 단일 필드 제약으로 선언할 수 없는 규칙에만 사용한다.
 */
class InvalidRequestParameterException(
    val parameterName: String,
    val reason: String,
) : RuntimeException(reason)

class InvalidRequestFieldException(
    val fieldName: String,
    val reason: String,
) : RuntimeException(reason)
