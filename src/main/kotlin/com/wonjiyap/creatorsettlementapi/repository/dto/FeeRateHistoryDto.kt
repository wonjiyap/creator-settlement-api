package com.wonjiyap.creatorsettlementapi.repository.dto

/** 수수료율 이력 단건 조회 파라미터: id 또는 effectiveMonth 정확 일치. */
data class FeeRateHistoryFetchOneParam(
    val id: String? = null,
    val effectiveMonth: String? = null,
)

/** 특정 연월("YYYY-MM")에 유효한 수수료율 조회 파라미터. */
data class EffectiveFeeRateParam(
    val month: String,
)
