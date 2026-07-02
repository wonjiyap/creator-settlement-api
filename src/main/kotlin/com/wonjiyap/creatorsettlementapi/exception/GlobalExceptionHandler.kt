package com.wonjiyap.creatorsettlementapi.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 도메인 예외: errorCode의 상태/메시지로 응답. */
    @ExceptionHandler(CreatorException::class)
    fun handle(e: CreatorException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(e.errorCode.code)
            .body(ErrorResponse(code = e.errorCode.code, message = e.message))

    /** 요청 본문 검증 실패(@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        badRequest(e.bindingResult.fieldErrors.joinToString { "${it.field}: ${it.defaultMessage}" })

    /** 쿼리 파라미터 타입 불일치(예: 날짜 형식 오류). */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        badRequest("요청 파라미터 형식이 올바르지 않습니다: ${e.name}")

    /** 필수 쿼리 파라미터 누락. */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        badRequest("필수 파라미터가 누락되었습니다: ${e.parameterName}")

    /** 읽을 수 없는 요청 본문(잘못된 JSON, 필수 필드 누락, 타입 불일치 등). */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        badRequest("요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요.")

    /** 매핑되지 않은 경로. */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(e: NoResourceFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.NOT_FOUND.code)
            .body(ErrorResponse(code = ErrorCode.NOT_FOUND.code, message = "요청한 경로를 찾을 수 없습니다: ${e.resourcePath}"))

    /** 그 밖의 예상치 못한 예외 → 500(내부 메시지는 노출하지 않고 로깅). */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.code)
            .body(ErrorResponse(code = ErrorCode.INTERNAL_SERVER_ERROR.code, message = ErrorCode.INTERNAL_SERVER_ERROR.message))
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.BAD_REQUEST.code)
            .body(ErrorResponse(code = ErrorCode.BAD_REQUEST.code, message = message))
}
