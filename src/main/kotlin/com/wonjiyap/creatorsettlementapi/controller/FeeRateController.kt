package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.FeeRateCreateRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.FeeRateResponse
import com.wonjiyap.creatorsettlementapi.service.FeeRateService
import com.wonjiyap.creatorsettlementapi.service.dto.FeeRateCreateParam
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "FeeRate", description = "수수료율 변경 이력 (운영자 성격 API — 이 과제에서는 인증 없이 일반 API로 제공)")
@RestController
@RequestMapping("/api/fee-rate")
class FeeRateController(
    private val feeRateService: FeeRateService,
) {

    /**
     * 수수료율 변경 등록. 다음 달 이후 월만 허용(소급 불가), 동일 적용 월 중복은 409.
     * 율은 백분율(0~100)로 받아 소수(scale 4)로 변환해 저장한다.
     */
    @PostMapping
    fun register(
        @Valid @RequestBody request: FeeRateCreateRequest,
    ) = FeeRateResponse.from(
        feeRateService.register(
            FeeRateCreateParam(
                effectiveMonth = request.effectiveMonth,
                feeRate = request.feeRatePercent.movePointLeft(2).setScale(4),
            ),
        ),
    )

    /** 수수료율 변경 이력 (적용 시작 월 내림차순). */
    @GetMapping("/list")
    fun list() = feeRateService.list().map { FeeRateResponse.from(it) }
}
