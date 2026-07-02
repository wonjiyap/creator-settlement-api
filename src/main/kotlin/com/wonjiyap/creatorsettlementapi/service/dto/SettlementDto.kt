package com.wonjiyap.creatorsettlementapi.service.dto

import java.math.BigDecimal

data class MonthlySettlementParam(
    val creatorId: String,
    val month: String,
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
