package com.wonjiyap.creatorsettlementapi.controller.dto

import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementResult
import java.math.BigDecimal

data class SettlementResponse(
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
) {
    companion object {
        fun from(result: MonthlySettlementResult) = SettlementResponse(
            creatorId = result.creatorId,
            month = result.month,
            totalSales = result.totalSales,
            totalRefund = result.totalRefund,
            netSales = result.netSales,
            feeRate = result.feeRate,
            fee = result.fee,
            settlementAmount = result.settlementAmount,
            saleCount = result.saleCount,
            cancelCount = result.cancelCount,
        )
    }
}
