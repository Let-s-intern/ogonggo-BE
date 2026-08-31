package com.ogonggo.adminapi.error

import com.ogonggo.core.error.BusinessException
import com.ogonggo.adminapi.response.ErrorResponse
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
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class AdminApiExceptionHandler {

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
        return ErrorResponse.from(AdminApiErrorCode.BAD_REQUEST).toResponseEntity()
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(exception: BusinessException): ResponseEntity<ErrorResponse> {
        log.error("handle: BusinessException", exception)
        return ErrorResponse.from(exception.errorCode).toResponseEntity()
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        log.error("handle: HttpRequestMethodNotSupportedException", exception)
        return ErrorResponse.from(AdminApiErrorCode.METHOD_NOT_ALLOWED).toResponseEntity()
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(exception: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        log.error("handle: NoResourceFoundException", exception)
        return ErrorResponse.from(AdminApiErrorCode.API_NOT_FOUND).toResponseEntity()
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(
        exception: ObjectOptimisticLockingFailureException,
    ): ResponseEntity<ErrorResponse> {
        log.error("handle: ObjectOptimisticLockingFailureException", exception)
        return ErrorResponse.from(AdminApiErrorCode.OPTIMISTIC_LOCK_CONFLICT).toResponseEntity()
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("handle: Exception", exception)
        return ErrorResponse.from(AdminApiErrorCode.INTERNAL_SERVER_ERROR).toResponseEntity()
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        ErrorResponse.from(AdminApiErrorCode.BAD_REQUEST, message).toResponseEntity()

    private fun validationMessage(errors: List<Pair<String, String?>>): String =
        errors
            .sortedWith(compareBy<Pair<String, String?>> { it.first }.thenBy { it.second.orEmpty() })
            .joinToString(", ") { (field, message) ->
                "[$field] ${message ?: AdminApiErrorCode.BAD_REQUEST.message}"
            }
            .ifBlank { AdminApiErrorCode.BAD_REQUEST.message }

    companion object {
        private val log = LoggerFactory.getLogger(AdminApiExceptionHandler::class.java)
    }
}
