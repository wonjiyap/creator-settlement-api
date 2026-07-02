package com.wonjiyap.creatorsettlementapi.service.dto

import java.math.BigDecimal

data class FeeRateCreateParam(
    val effectiveMonth: String,
    val feeRate: BigDecimal,
)
