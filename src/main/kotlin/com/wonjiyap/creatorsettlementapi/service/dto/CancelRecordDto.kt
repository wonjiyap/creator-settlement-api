package com.wonjiyap.creatorsettlementapi.service.dto

import java.time.LocalDateTime

data class CancelRecordCreateParam(
    val saleRecordId: String,
    val refundAmount: Long,
    val canceledAt: LocalDateTime,
)
