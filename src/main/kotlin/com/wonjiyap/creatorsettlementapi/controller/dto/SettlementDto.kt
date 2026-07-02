package com.wonjiyap.creatorsettlementapi.controller.dto

import com.wonjiyap.creatorsettlementapi.service.dto.CreatorSettlement
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementResult
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementResult
import java.math.BigDecimal
import java.time.LocalDate

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

data class SettlementSummaryResponse(
    val from: LocalDate,
    val to: LocalDate,
    val feeRate: BigDecimal,
    val totalSettlementAmount: Long,
    val creators: List<CreatorSettlementResponse>,
) {
    companion object {
        fun from(result: PeriodSettlementResult) = SettlementSummaryResponse(
            from = result.from,
            to = result.to,
            feeRate = result.feeRate,
            totalSettlementAmount = result.totalSettlementAmount,
            creators = result.creators.map { CreatorSettlementResponse.from(it) },
        )
    }
}

data class CreatorSettlementResponse(
    val creatorId: String,
    val totalSales: Long,
    val totalRefund: Long,
    val netSales: Long,
    val fee: Long,
    val settlementAmount: Long,
    val saleCount: Long,
    val cancelCount: Long,
) {
    companion object {
        fun from(c: CreatorSettlement) = CreatorSettlementResponse(
            creatorId = c.creatorId,
            totalSales = c.totalSales,
            totalRefund = c.totalRefund,
            netSales = c.netSales,
            fee = c.fee,
            settlementAmount = c.settlementAmount,
            saleCount = c.saleCount,
            cancelCount = c.cancelCount,
        )
    }
}
