package com.wonjiyap.creatorsettlementapi.controller.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.wonjiyap.creatorsettlementapi.domain.Settlement
import com.wonjiyap.creatorsettlementapi.domain.SettlementStatus
import com.wonjiyap.creatorsettlementapi.service.dto.CreatorSettlement
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementResult
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementResult
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

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

data class SettlementCreateRequest(
    @field:NotBlank
    val creatorId: String,
    @field:NotBlank
    val month: String,
)

data class SettlementDetailResponse(
    val id: String,
    val creatorId: String,
    val month: String,
    val status: SettlementStatus,
    val totalSales: Long,
    val totalRefund: Long,
    val netSales: Long,
    val feeRate: BigDecimal,
    val fee: Long,
    val settlementAmount: Long,
    val saleCount: Long,
    val cancelCount: Long,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val confirmedAt: LocalDateTime?,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val paidAt: LocalDateTime?,
) {
    companion object {
        fun from(settlement: Settlement) = SettlementDetailResponse(
            id = settlement.id,
            creatorId = settlement.creatorId,
            month = settlement.period,
            status = settlement.status,
            totalSales = settlement.totalSales,
            totalRefund = settlement.totalRefund,
            netSales = settlement.netSales,
            feeRate = settlement.feeRate,
            fee = settlement.fee,
            settlementAmount = settlement.settlementAmount,
            saleCount = settlement.saleCount,
            cancelCount = settlement.cancelCount,
            createdAt = settlement.createdAt,
            confirmedAt = settlement.confirmedAt,
            paidAt = settlement.paidAt,
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
