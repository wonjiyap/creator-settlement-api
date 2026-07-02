package com.wonjiyap.creatorsettlementapi.service.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class SaleRecordCreateParam(
    val courseId: String,
    val studentId: String,
    val amount: Long,
    val paidAt: LocalDateTime,
)

/** 판매 목록 조회 파라미터: 크리에이터·기간 모두 선택(없으면 전체). */
data class SaleRecordListParam(
    val creatorId: String? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
)