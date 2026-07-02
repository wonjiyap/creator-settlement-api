package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@Transactional
class SettlementServiceTest(
    @Autowired val settlementService: SettlementService,
) {

    @Test
    fun `명세 시나리오 - creator-1 2025-03 정산`() {
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-1", "2025-03"))

        assertEquals(260000, result.totalSales)
        assertEquals(110000, result.totalRefund)
        assertEquals(150000, result.netSales)
        assertEquals(30000, result.fee)
        assertEquals(120000, result.settlementAmount)
        assertEquals(4, result.saleCount)
        assertEquals(2, result.cancelCount)
    }

    @Test
    fun `빈 월 - creator-3 2025-03 은 모두 0`() {
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-3", "2025-03"))

        assertEquals(0, result.totalSales)
        assertEquals(0, result.totalRefund)
        assertEquals(0, result.netSales)
        assertEquals(0, result.fee)
        assertEquals(0, result.settlementAmount)
        assertEquals(0, result.saleCount)
        assertEquals(0, result.cancelCount)
    }

    @Test
    fun `월 경계 - 1월 판매는 1월에만 반영된다`() {
        // sale-5(60,000)은 2025-01-31 판매 → 1월 정산에 판매로만 반영
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-2", "2025-01"))

        assertEquals(60000, result.totalSales)
        assertEquals(0, result.totalRefund)
        assertEquals(1, result.saleCount)
        assertEquals(0, result.cancelCount)
        assertEquals(48000, result.settlementAmount) // 60,000 - 수수료 12,000
    }

    @Test
    fun `월 경계 + 음수 - 2월은 취소만 반영되어 순매출 음수`() {
        // cancel-3(60,000)은 2025-02-02 취소, 2월엔 판매 없음 → 순매출 -60,000
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-2", "2025-02"))

        assertEquals(0, result.totalSales)
        assertEquals(60000, result.totalRefund)
        assertEquals(-60000, result.netSales)
        assertEquals(-12000, result.fee)
        assertEquals(-48000, result.settlementAmount)
        assertEquals(0, result.saleCount)
        assertEquals(1, result.cancelCount)
    }

    @Test
    fun `다건 볼륨 - creator-4 2025-06`() {
        // 판매 4건 320,000, 환불 135,000 → 순 185,000, 수수료 37,000, 정산 148,000
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-4", "2025-06"))

        assertEquals(320000, result.totalSales)
        assertEquals(135000, result.totalRefund)
        assertEquals(185000, result.netSales)
        assertEquals(37000, result.fee)
        assertEquals(148000, result.settlementAmount)
        assertEquals(4, result.saleCount)
        assertEquals(2, result.cancelCount)
    }

    @Test
    fun `잘못된 연월 형식이면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            settlementService.getMonthly(MonthlySettlementParam("creator-1", "2025-13"))
        }
    }
}
