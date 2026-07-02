package com.wonjiyap.creatorsettlementapi.repository.dto

import java.time.LocalDateTime

data class SaleRecordFetchOneParam(
    val id: String? = null,
)

/** 판매 목록 조회 파라미터: 크리에이터·기간(paidAt) 모두 선택. */
data class SaleRecordFetchParam(
    val creatorId: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
)
