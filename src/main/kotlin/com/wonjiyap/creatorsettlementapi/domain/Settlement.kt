package com.wonjiyap.creatorsettlementapi.domain

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDateTime

/** 정산 상태: PENDING → CONFIRMED → PAID 단방향 전이만 허용. */
enum class SettlementStatus { PENDING, CONFIRMED, PAID }

/**
 * 크리에이터 + 월(YYYY-MM) 단위 정산 확정 스냅샷.
 * 금액은 생성 시점 계산으로 고정되며 이후 판매/취소 추가를 반영하지 않는다.
 */
@Entity
class Settlement(
    @Id
    val id: String,
    val creatorId: String,
    /** 정산 대상 연월 "YYYY-MM" (H2에서 MONTH는 예약어라 period). */
    val period: String,
    @Enumerated(EnumType.STRING)
    var status: SettlementStatus = SettlementStatus.PENDING,
    val totalSales: Long,
    val totalRefund: Long,
    val netSales: Long,
    /** 생성 당시 수수료율 스냅샷 (수수료율 변경 이력 확장 대비). */
    @Column(precision = 5, scale = 4)
    val feeRate: BigDecimal,
    val fee: Long,
    val settlementAmount: Long,
    val saleCount: Long,
    val cancelCount: Long,
    val createdAt: LocalDateTime,
    var confirmedAt: LocalDateTime? = null,
    var paidAt: LocalDateTime? = null,
) {
    fun confirm(at: LocalDateTime) {
        if (status != SettlementStatus.PENDING) {
            throw CreatorException(ErrorCode.CONFLICT, "PENDING 상태의 정산만 확정할 수 있습니다: 현재 $status")
        }
        status = SettlementStatus.CONFIRMED
        confirmedAt = at
    }

    fun pay(at: LocalDateTime) {
        if (status != SettlementStatus.CONFIRMED) {
            throw CreatorException(ErrorCode.CONFLICT, "CONFIRMED 상태의 정산만 지급 처리할 수 있습니다: 현재 $status")
        }
        status = SettlementStatus.PAID
        paidAt = at
    }
}
