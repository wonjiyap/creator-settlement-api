package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.SaleRecordCreateRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.SaleRecordResponse
import com.wonjiyap.creatorsettlementapi.service.SaleRecordService
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "SaleRecord", description = "판매 내역")
@RestController
@RequestMapping("/api/sale-record")
class SaleRecordController(
    private val saleService: SaleRecordService,
) {

    @PostMapping
    fun register(
        @Valid @RequestBody request: SaleRecordCreateRequest,
    ) = SaleRecordResponse.from(
        saleService.register(
            param = SaleRecordCreateParam(
                courseId = request.courseId,
                studentId = request.studentId,
                amount = request.amount,
                paidAt = request.paidAt,
            )
        )
    )
}
