package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.CancelRecordCreateRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.CancelRecordResponse
import com.wonjiyap.creatorsettlementapi.service.CancelRecordService
import com.wonjiyap.creatorsettlementapi.service.dto.CancelRecordCreateParam
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CancelRecord", description = "취소(환불) 내역")
@RestController
@RequestMapping("/api/cancel-record")
class CancelRecordController(
    private val cancelService: CancelRecordService,
) {

    @PostMapping
    fun register(
        @Valid @RequestBody request: CancelRecordCreateRequest,
    ) = CancelRecordResponse.from(
        cancelService.register(
            param = CancelRecordCreateParam(
                saleRecordId = request.saleRecordId,
                refundAmount = request.refundAmount,
                canceledAt = request.canceledAt,
            ),
        ),
    )
}
