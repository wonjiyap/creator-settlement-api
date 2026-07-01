package com.wonjiyap.creatorsettlementapi.service.dto

import java.time.LocalDateTime

data class SaleRecordCreateParam(
    val courseId: String,
    val studentId: String,
    val amount: Long,
    val paidAt: LocalDateTime,
)