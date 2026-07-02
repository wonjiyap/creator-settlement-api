package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementResponse
import com.wonjiyap.creatorsettlementapi.service.SettlementService
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
}
