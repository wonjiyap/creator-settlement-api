package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.SaleRecordCreateRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.SaleRecordResponse
import com.wonjiyap.creatorsettlementapi.service.SaleRecordService
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordListParam
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
            ),
        ),
    )

    @GetMapping("/list")
    fun list(
        @RequestParam(name = "creator_id", required = false) creatorId: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ) = saleService.list(
        SaleRecordListParam(
            creatorId = creatorId,
            from = from,
            to = to,
        )
    ).map { SaleRecordResponse.from(it) }
}
