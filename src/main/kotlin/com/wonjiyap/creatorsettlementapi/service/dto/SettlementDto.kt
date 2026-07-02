package com.wonjiyap.creatorsettlementapi.service.dto

import java.math.BigDecimal
import java.time.LocalDate

data class MonthlySettlementParam(
    val creatorId: String,
    val month: String,
)

data class PeriodSettlementParam(
    val from: LocalDate,
    val to: LocalDate,
)

/** 운영자 기간 집계 결과: 크리에이터별 정산 목록 + 전체 정산 합계. */
data class PeriodSettlementResult(
    val from: LocalDate,
    val to: LocalDate,
    val feeRate: BigDecimal,
    val totalSettlementAmount: Long,
    val creators: List<CreatorSettlement>,
)

data class CreatorSettlement(
    val creatorId: String,
    val totalSales: Long,
    val totalRefund: Long,
    val netSales: Long,
    val fee: Long,
    val settlementAmount: Long,
    val saleCount: Long,
    val cancelCount: Long,
)

data class MonthlySettlementResult(
    val creatorId: String,
    val month: String,
    val totalSales: Long,
    val totalRefund: Long,
    val netSales: Long,
    val feeRate: BigDecimal,
    val fee: Long,
    val settlementAmount: Long,
    val saleCount: Long,
    val cancelCount: Long,
)
