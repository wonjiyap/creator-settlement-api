package com.wonjiyap.creatorsettlementapi.controller.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.wonjiyap.creatorsettlementapi.domain.FeeRateHistory
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDateTime

data class FeeRateCreateRequest(
    @field:NotBlank
    val effectiveMonth: String,
    /** 백분율 입력(0 이상 100 미만, 소수점 둘째 자리까지). 예: 12.5 → 12.5% (0.1250으로 저장) */
    @field:DecimalMin("0.0")
    @field:DecimalMax(value = "100.0", inclusive = false)
    @field:Digits(integer = 3, fraction = 2)
    val feeRatePercent: BigDecimal,
)

data class FeeRateResponse(
    val id: String,
    val effectiveMonth: String,
    /** 저장 단위(소수). 예: 0.1250 */
    val feeRate: BigDecimal,
    /** 백분율 표기. 예: 12.50 */
    val feeRatePercent: BigDecimal,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(history: FeeRateHistory) = FeeRateResponse(
            id = history.id,
            effectiveMonth = history.effectiveMonth,
            feeRate = history.feeRate,
            feeRatePercent = history.feeRate.movePointRight(2).setScale(2),
            createdAt = history.createdAt,
        )
    }
}
