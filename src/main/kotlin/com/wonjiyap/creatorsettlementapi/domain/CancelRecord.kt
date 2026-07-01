package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class CancelRecord(
    @Id
    val id: String,
    val saleRecordId: String,
    val refundAmount: Long,
    val canceledAt: LocalDateTime,
)
