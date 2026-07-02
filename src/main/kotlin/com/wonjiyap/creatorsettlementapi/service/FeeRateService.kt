package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.domain.FeeRateHistory
import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.FeeRateHistoryRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.FeeRateHistoryFetchOneParam
import com.wonjiyap.creatorsettlementapi.service.dto.FeeRateCreateParam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.UUID

/** 수수료율 변경 이력 관리. 과거 정산의 불변을 위해 소급 등록(현재 월 이하)은 허용하지 않는다. */
@Service
class FeeRateService(
    private val feeRateHistoryRepository: FeeRateHistoryRepository,
) {

    /**
     * 수수료율 변경 등록. 적용 시작 월은 다음 달 이후만 허용(400),
     * 같은 적용 월에 이미 등록된 율이 있으면 409. (사전 조회 + 유니크 제약 이중 방어)
     */
    @Transactional
    fun register(param: FeeRateCreateParam): FeeRateHistory {
        val effectiveMonth = parseMonth(param.effectiveMonth)
        if (!effectiveMonth.isAfter(YearMonth.now())) {
            throw CreatorException(
                ErrorCode.BAD_REQUEST,
                "수수료율은 다음 달 이후부터 적용할 수 있습니다(소급 변경 불가): $effectiveMonth",
            )
        }

        feeRateHistoryRepository.fetchOne(FeeRateHistoryFetchOneParam(effectiveMonth = effectiveMonth.toString()))
            ?.let {
                throw CreatorException(
                    ErrorCode.CONFLICT,
                    "이미 해당 월부터 적용되는 수수료율이 존재합니다: $effectiveMonth (율: ${it.feeRate})",
                )
            }

        return feeRateHistoryRepository.save(
            FeeRateHistory(
                id = "fee-rate-${UUID.randomUUID()}",
                effectiveMonth = effectiveMonth.toString(),
                feeRate = param.feeRate,
                createdAt = LocalDateTime.now(),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun list(): List<FeeRateHistory> = feeRateHistoryRepository.fetch()

    private fun parseMonth(month: String): YearMonth =
        try {
            YearMonth.parse(month)
        } catch (e: DateTimeParseException) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "잘못된 연월 형식입니다: $month (예: 2025-03)")
        }
}
