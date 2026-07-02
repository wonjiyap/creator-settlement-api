package com.wonjiyap.creatorsettlementapi.exception

enum class ErrorCode(
    val code: Int,
    val message: String,
) {
    BAD_REQUEST(code = 400, message = "잘못된 요청입니다"),
    UNAUTHORIZED(code = 401, message = "인증이 필요합니다"),
    FORBIDDEN(code = 403, message = "권한이 없습니다"),
    NOT_FOUND(code = 404, message = "리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(code = 405, message = "지원하지 않는 HTTP 메서드입니다"),
    CONFLICT(code = 409, message = "이미 존재하는 리소스입니다"),
    INTERNAL_SERVER_ERROR(code = 500, message = "서버 오류가 발생했습니다"),
}
