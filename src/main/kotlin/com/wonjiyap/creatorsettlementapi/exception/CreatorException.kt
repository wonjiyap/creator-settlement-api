package com.wonjiyap.creatorsettlementapi.exception

open class CreatorException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
