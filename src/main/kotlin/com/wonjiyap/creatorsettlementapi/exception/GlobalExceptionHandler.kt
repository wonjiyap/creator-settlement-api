package com.wonjiyap.creatorsettlementapi.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

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
}
