package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 수수료율 변경 이력. [effectiveMonth]("YYYY-MM")부터 다음 변경 전까지 [feeRate]가 적용된다.
 * 과거 정산의 불변을 위해 소급 등록(현재 월 이전)은 허용하지 않는다.
 */
@Entity
class FeeRateHistory(
    @Id
    val id: String,
    val effectiveMonth: String,
    @Column(precision = 5, scale = 4)
    val feeRate: BigDecimal,
    val createdAt: LocalDateTime,
)
