package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.repository.FeeRateHistoryRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.EffectiveFeeRateParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/**
 * 플랫폼 수수료 정책. 수수료율은 월 단위 변경 이력(fee_rate_history)으로 관리하며,
 * 연월마다 해당 시점에 유효한 율을 적용한다. 이력이 없는 구간은 설정(`settlement.fee-rate`) 기본값.
 */
@Component
class FeePolicy(
    @Value("\${settlement.fee-rate:0.20}")
    private val defaultFeeRate: BigDecimal,
    private val feeRateHistoryRepository: FeeRateHistoryRepository,
) {
    /**
     * 해당 연월에 유효한 수수료율(effectiveMonth <= 연월 중 최신).
     * scale 4로 정규화해 반환한다(DB DECIMAL(5,4)와 동일 — 율 비교/그룹핑 시 scale 차이 방지).
     */
    fun rateFor(yearMonth: YearMonth): BigDecimal =
        (feeRateHistoryRepository.fetchEffective(EffectiveFeeRateParam(yearMonth.toString()))?.feeRate ?: defaultFeeRate)
            .setScale(4)

    /** 순 판매액에 수수료율을 적용한 수수료(원 단위, 반올림). */
    fun calculateFee(netSales: Long, feeRate: BigDecimal): Long =
        netSales.toBigDecimal()
            .multiply(feeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
}
