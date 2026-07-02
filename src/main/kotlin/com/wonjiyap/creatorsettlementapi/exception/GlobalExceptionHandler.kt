package com.wonjiyap.creatorsettlementapi.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CreatorException::class)
    fun handle(e: CreatorException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(e.errorCode.code)
            .body(ErrorResponse(code = e.errorCode.code, message = e.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.BAD_REQUEST.code)
            .body(
                ErrorResponse(
                    code = ErrorCode.BAD_REQUEST.code,
                    message = e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" },
                ),
            )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.BAD_REQUEST.code)
            .body(
                ErrorResponse(
                    code = ErrorCode.BAD_REQUEST.code,
                    message = "요청 파라미터 형식이 올바르지 않습니다: ${e.name}",
                ),
            )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.BAD_REQUEST.code)
            .body(
                ErrorResponse(
                    code = ErrorCode.BAD_REQUEST.code,
                    message = "필수 파라미터가 누락되었습니다: ${e.parameterName}",
                ),
            )
}
