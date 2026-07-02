package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementParam
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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

    @Test
    fun `존재하지 않는 크리에이터면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            settlementService.getMonthly(MonthlySettlementParam("creator-없음", "2025-03"))
        }
    }

    @Test
    fun `기간 집계 - 2025-03 전체 크리에이터 정산`() {
        // 2025-03 활동: creator-1(정산 120,000), creator-2(60,000 판매 → 정산 48,000),
        //               creator-4(순 280,000 → 정산 224,000). creator-3/5는 3월 활동 없음.
        val result = settlementService.getSummary(
            PeriodSettlementParam(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)),
        )

        assertEquals(3, result.creators.size)
        assertEquals(392000, result.totalSettlementAmount) // 120,000 + 48,000 + 224,000
        assertEquals(120000, result.creators.first { it.creatorId == "creator-1" }.settlementAmount)
    }

    @Test
    fun `기간 집계 - 월을 가로지르는 자유 구간`() {
        // 2025-02-01 ~ 2025-03-31 (2월+3월). 2월엔 creator-3 sale-7(2/14, 120,000), creator-2 cancel-3(2/2, 60,000)이 포함된다.
        //  creator-1: 120,000 / creator-2: 순 0(판매 60,000 - 환불 60,000) → 0 / creator-3: 순 120,000 → 96,000 / creator-4: 224,000
        val result = settlementService.getSummary(
            PeriodSettlementParam(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 31)),
        )

        assertEquals(4, result.creators.size)
        assertEquals(440000, result.totalSettlementAmount) // 120,000 + 0 + 96,000 + 224,000
        assertEquals(0, result.creators.first { it.creatorId == "creator-2" }.settlementAmount)
        assertEquals(96000, result.creators.first { it.creatorId == "creator-3" }.settlementAmount)
    }

    @Test
    fun `기간 집계 - 시작일이 종료일보다 늦으면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            settlementService.getSummary(
                PeriodSettlementParam(LocalDate.of(2025, 3, 31), LocalDate.of(2025, 3, 1)),
            )
        }
    }

    @Test
    fun `추가 시나리오 - creator-4 2025-03 (한 판매 다수 부분취소 + 같은 날 취소)`() {
        // sale-105에 부분취소 2건(cancel-103,104), sale-103과 cancel-102가 같은 날(3/18) — 모두 3월 집계에 반영
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-4", "2025-03"))

        assertEquals(440000, result.totalSales)
        assertEquals(160000, result.totalRefund) // 70,000 + 40,000 + 30,000 + 20,000
        assertEquals(280000, result.netSales)
        assertEquals(56000, result.fee)
        assertEquals(224000, result.settlementAmount)
        assertEquals(5, result.saleCount)
        assertEquals(4, result.cancelCount)
    }

    @Test
    fun `추가 시나리오 - creator-4 2025-04 (월 시작·끝 경계 시각 포함)`() {
        // sale-106(04-01 00:00:00), sale-107(04-30 23:59:59) 둘 다 4월에 포함되어야 한다
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-4", "2025-04"))

        assertEquals(160000, result.totalSales)
        assertEquals(2, result.saleCount)
        assertEquals(0, result.totalRefund)
        assertEquals(128000, result.settlementAmount)
    }

    @Test
    fun `추가 시나리오 - creator-4 2025-05 (크로스먼스 전액환불 → 순매출 음수)`() {
        // cancel-105(5/1)는 4월 판매 sale-106의 환불 → 5월엔 판매 0, 환불 70,000
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-4", "2025-05"))

        assertEquals(0, result.totalSales)
        assertEquals(70000, result.totalRefund)
        assertEquals(-70000, result.netSales)
        assertEquals(-14000, result.fee)
        assertEquals(-56000, result.settlementAmount)
    }

    @Test
    fun `추가 시나리오 - creator-5 2025-05 (동월 전액환불 반영)`() {
        // sale-109(150,000)가 같은 달 cancel-106(150,000)으로 전액환불 → 환불에 그대로 반영(sale-108은 유지)
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-5", "2025-05"))

        assertEquals(300000, result.totalSales)
        assertEquals(150000, result.totalRefund)
        assertEquals(150000, result.netSales)
        assertEquals(120000, result.settlementAmount)
        assertEquals(2, result.saleCount)
        assertEquals(1, result.cancelCount)
    }

    @Test
    fun `추가 시나리오 - creator-5 2025-06 (재구매는 다음 달에 독립 집계)`() {
        // student-15가 5월(sale-108)에 이어 6월(sale-110)에 course-9 재구매 → 6월에 독립 집계
        val result = settlementService.getMonthly(MonthlySettlementParam("creator-5", "2025-06"))

        assertEquals(150000, result.totalSales)
        assertEquals(1, result.saleCount)
        assertEquals(120000, result.settlementAmount)
    }
}
