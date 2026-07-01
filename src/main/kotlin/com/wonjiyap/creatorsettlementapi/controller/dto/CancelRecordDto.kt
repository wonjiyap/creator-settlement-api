package com.wonjiyap.creatorsettlementapi.controller.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.wonjiyap.creatorsettlementapi.domain.CancelRecord
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class CancelRecordResponse(
    val id: String,
    val saleRecordId: String,
    val refundAmount: Long,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val canceledAt: LocalDateTime,
) {
    companion object {
        fun from(cancel: CancelRecord) = CancelRecordResponse(
            id = cancel.id,
            saleRecordId = cancel.saleRecordId,
            refundAmount = cancel.refundAmount,
            canceledAt = cancel.canceledAt,
        )
    }
}

data class CancelRecordCreateRequest(
    @field:NotBlank
    val saleRecordId: String,
    @field:Positive
    val refundAmount: Long,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val canceledAt: LocalDateTime,
)
