package com.wonjiyap.creatorsettlementapi.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class SaleRecord(
    @Id
    val id: String,
    val courseId: String,
    val studentId: String,
    val amount: Long,
    val paidAt: LocalDateTime,
)
