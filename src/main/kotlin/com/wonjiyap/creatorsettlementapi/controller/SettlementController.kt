package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementCreateRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementDetailResponse
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementResponse
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementSummaryResponse
import com.wonjiyap.creatorsettlementapi.domain.SettlementStatus
import com.wonjiyap.creatorsettlementapi.service.SettlementService
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementListParam
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Settlement", description = "정산")
@RestController
@RequestMapping("/api/settlement")
class SettlementController(
    private val settlementService: SettlementService,
) {

    @GetMapping("/monthly")
    fun getMonthly(
        @RequestParam("creator_id") creatorId: String,
        @RequestParam month: String,
    ) = SettlementResponse.from(
        settlementService.getMonthly(
            MonthlySettlementParam(creatorId = creatorId, month = month),
        ),
    )

    @GetMapping("/summary")
    fun getSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ) = SettlementSummaryResponse.from(
        settlementService.getSummary(
            PeriodSettlementParam(from = from, to = to),
        ),
    )

    /** 정산 스냅샷 생성(PENDING). 동일 크리에이터+월 중복 시 409. */
    @PostMapping
    fun create(
        @Valid @RequestBody request: SettlementCreateRequest,
    ) = SettlementDetailResponse.from(
        settlementService.create(
            SettlementCreateParam(
                creatorId = request.creatorId,
                month = request.month,
            ),
        ),
    )

    /** PENDING → CONFIRMED. */
    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: String,
    ) = SettlementDetailResponse.from(settlementService.confirm(id))

    /** CONFIRMED → PAID. */
    @PostMapping("/{id}/pay")
    fun pay(
        @PathVariable id: String,
    ) = SettlementDetailResponse.from(settlementService.pay(id))

    @GetMapping("/list")
    fun list(
        @RequestParam("creator_id", required = false) creatorId: String?,
        @RequestParam(required = false) month: String?,
        @RequestParam(required = false) status: SettlementStatus?,
    ) = settlementService.list(
        SettlementListParam(
            creatorId = creatorId,
            month = month,
            status = status,
        ),
    ).map { SettlementDetailResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ) = SettlementDetailResponse.from(settlementService.get(id))
}
