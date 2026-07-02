package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.service.dto.FeeRateCreateParam
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class FeeRateServiceTest(
    @Autowired val feeRateService: FeeRateService,
) {

    private fun nextMonth(): String = YearMonth.now().plusMonths(1).toString()

    @Test
    fun `등록 성공 - 다음 달 이후 적용 월은 등록된다`() {
        val history = feeRateService.register(FeeRateCreateParam(nextMonth(), BigDecimal("0.18")))

        assertTrue(history.id.startsWith("fee-rate-"))
        assertEquals(nextMonth(), history.effectiveMonth)
        assertEquals(0, BigDecimal("0.18").compareTo(history.feeRate))
    }

    @Test
    fun `등록 실패 - 현재 월은 소급으로 간주되어 400`() {
        val e = assertThrows(CreatorException::class.java) {
            feeRateService.register(FeeRateCreateParam(YearMonth.now().toString(), BigDecimal("0.18")))
        }
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode)
    }

    @Test
    fun `등록 실패 - 과거 월은 400 (과거 정산 불변 보장)`() {
        val e = assertThrows(CreatorException::class.java) {
            feeRateService.register(FeeRateCreateParam("2025-01", BigDecimal("0.18")))
        }
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode)
    }

    @Test
    fun `등록 실패 - 잘못된 연월 형식이면 400`() {
        val e = assertThrows(CreatorException::class.java) {
            feeRateService.register(FeeRateCreateParam("2026-13", BigDecimal("0.18")))
        }
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode)
    }

    @Test
    fun `등록 실패 - 동일 적용 월 중복 등록은 409`() {
        feeRateService.register(FeeRateCreateParam(nextMonth(), BigDecimal("0.18")))

        val e = assertThrows(CreatorException::class.java) {
            feeRateService.register(FeeRateCreateParam(nextMonth(), BigDecimal("0.10")))
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    @Test
    fun `이력 조회 - 시드 포함 적용 월 내림차순으로 반환된다`() {
        val registered = feeRateService.register(FeeRateCreateParam(nextMonth(), BigDecimal("0.18")))

        val histories = feeRateService.list()

        // V6 시드(fee-rate-1: 2024-01 / fee-rate-2: 2025-07) + 방금 등록분
        assertTrue(histories.map { it.id }.containsAll(listOf("fee-rate-1", "fee-rate-2", registered.id)))
        assertEquals(histories.map { it.effectiveMonth }.sortedDescending(), histories.map { it.effectiveMonth })
    }
}
