package com.wonjiyap.creatorsettlementapi.repository.dto

import java.time.LocalDateTime

/** 크리에이터 + 기간(from ~ to, 경계 포함) 조회 파라미터. */
data class CreatorPeriodParam(
    val creatorId: String,
    val from: LocalDateTime,
    val to: LocalDateTime,
)

/** 금액 합계 + 건수 집계 프로젝션. */
data class AmountCount(
    val total: Long,
    val count: Long,
)
