package com.wonjiyap.creatorsettlementapi.controller.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class SaleRecordResponse(
    val id: String,
    val courseId: String,
    val courseTitle: String?,
    val creatorId: String?,
    val creatorName: String?,
    val studentId: String,
    val amount: Long,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val paidAt: LocalDateTime,
) {
    companion object {
        fun from(sale: SaleRecord) = SaleRecordResponse(
            id = sale.id,
            courseId = sale.courseId,
            courseTitle = sale.course?.title,
            creatorId = sale.course?.creator?.id,
            creatorName = sale.course?.creator?.name,
            studentId = sale.studentId,
            amount = sale.amount,
            paidAt = sale.paidAt,
        )
    }
}

data class SaleRecordCreateRequest(
    @field:NotBlank
    val courseId: String,
    @field:NotBlank
    val studentId: String,
    @field:Positive
    val amount: Long,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val paidAt: LocalDateTime,
)