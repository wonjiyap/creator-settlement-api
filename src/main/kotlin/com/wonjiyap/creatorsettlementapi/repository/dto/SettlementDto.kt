package com.wonjiyap.creatorsettlementapi.repository.dto

import com.wonjiyap.creatorsettlementapi.domain.SettlementStatus
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

/** 기간(from ~ to, 경계 포함) 조회 파라미터 (전체 크리에이터 대상). */
data class PeriodParam(
    val from: LocalDateTime,
    val to: LocalDateTime,
)

/** 크리에이터별 금액 합계 + 건수 집계 프로젝션 (GROUP BY creatorId). */
data class CreatorAmountCount(
    val creatorId: String,
    val total: Long,
    val count: Long,
)

/** 정산 단건 조회 파라미터: id 또는 (creatorId + period). */
data class SettlementFetchOneParam(
    val id: String? = null,
    val creatorId: String? = null,
    val period: String? = null,
)

/** 정산 목록 조회 파라미터 (모두 선택, 독립 적용). */
data class SettlementFetchParam(
    val creatorId: String? = null,
    val period: String? = null,
    val status: SettlementStatus? = null,
)
