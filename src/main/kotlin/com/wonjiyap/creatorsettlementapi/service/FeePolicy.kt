package com.wonjiyap.creatorsettlementapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 플랫폼 수수료 정책. 현재 수수료율은 고정(20%)이지만 설정(`settlement.fee-rate`)으로 분리해
 * 변경 가능하도록 한다. (추후 수수료율 이력 관리로 확장 여지)
 */
@Component
class FeePolicy(
    @Value("\${settlement.fee-rate:0.20}")
    val feeRate: BigDecimal,
) {
    /** 순 판매액에 수수료율을 적용한 수수료(원 단위, 반올림). */
    fun calculateFee(netSales: Long): Long =
        netSales.toBigDecimal()
            .multiply(feeRate)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
}
