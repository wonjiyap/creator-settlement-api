package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.domain.SettlementStatus
import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementListParam
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class SettlementServiceTest(
    @Autowired val settlementService: SettlementService,
    @Autowired val saleRecordService: SaleRecordService,
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

    // ---- 정산 상태 관리 & 중복 정산 방지 ----

    @Test
    fun `정산 생성 - creator-1 2025-03 스냅샷이 PENDING으로 저장된다`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))

        assertTrue(settlement.id.startsWith("settlement-"))
        assertEquals("creator-1", settlement.creatorId)
        assertEquals("2025-03", settlement.period)
        assertEquals(SettlementStatus.PENDING, settlement.status)
        assertEquals(260000, settlement.totalSales)
        assertEquals(110000, settlement.totalRefund)
        assertEquals(150000, settlement.netSales)
        assertEquals(30000, settlement.fee)
        assertEquals(120000, settlement.settlementAmount)
        assertEquals(4, settlement.saleCount)
        assertEquals(2, settlement.cancelCount)
        // DECIMAL(5,4) 재조회 시 scale이 달라질 수 있으므로 compareTo로 비교
        assertEquals(0, BigDecimal("0.20").compareTo(settlement.feeRate))
        assertNull(settlement.confirmedAt)
        assertNull(settlement.paidAt)
    }

    @Test
    fun `정산 생성 - 빈 월(creator-3)도 0원으로 생성된다`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-3", "2025-03"))

        assertEquals(SettlementStatus.PENDING, settlement.status)
        assertEquals(0, settlement.totalSales)
        assertEquals(0, settlement.totalRefund)
        assertEquals(0, settlement.netSales)
        assertEquals(0, settlement.fee)
        assertEquals(0, settlement.settlementAmount)
    }

    @Test
    fun `정산 생성 - 순매출 음수 월(creator-4 2025-05)도 그대로 저장된다`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-4", "2025-05"))

        assertEquals(-70000, settlement.netSales)
        assertEquals(-56000, settlement.settlementAmount)
    }

    @Test
    fun `중복 정산 방지 - 동일 크리에이터+월 재생성은 409`() {
        settlementService.create(SettlementCreateParam("creator-1", "2025-03"))

        val e = assertThrows(CreatorException::class.java) {
            settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    @Test
    fun `중복 정산 방지 - 확정된 정산이 있어도 상태와 무관하게 409`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        settlementService.confirm(settlement.id)

        val e = assertThrows(CreatorException::class.java) {
            settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    @Test
    fun `정산 생성 - 존재하지 않는 크리에이터면 404`() {
        val e = assertThrows(CreatorException::class.java) {
            settlementService.create(SettlementCreateParam("creator-없음", "2025-03"))
        }
        assertEquals(ErrorCode.NOT_FOUND, e.errorCode)
    }

    @Test
    fun `정산 생성 - 잘못된 연월 형식이면 400`() {
        val e = assertThrows(CreatorException::class.java) {
            settlementService.create(SettlementCreateParam("creator-1", "2025-13"))
        }
        assertEquals(ErrorCode.BAD_REQUEST, e.errorCode)
    }

    @Test
    fun `상태 전이 - confirm 시 CONFIRMED로 전이되고 시각이 기록된다`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))

        val confirmed = settlementService.confirm(settlement.id)

        assertEquals(SettlementStatus.CONFIRMED, confirmed.status)
        assertNotNull(confirmed.confirmedAt)
        assertNull(confirmed.paidAt)
    }

    @Test
    fun `상태 전이 - confirm 후 pay 시 PAID로 전이되고 시각이 기록된다`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        settlementService.confirm(settlement.id)

        val paid = settlementService.pay(settlement.id)

        assertEquals(SettlementStatus.PAID, paid.status)
        assertNotNull(paid.confirmedAt)
        assertNotNull(paid.paidAt)
    }

    @Test
    fun `상태 전이 - PENDING에서 바로 pay는 409 (건너뛰기 금지)`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))

        val e = assertThrows(CreatorException::class.java) {
            settlementService.pay(settlement.id)
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    @Test
    fun `상태 전이 - PAID에서 confirm은 409 (역행 금지)`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        settlementService.confirm(settlement.id)
        settlementService.pay(settlement.id)

        val e = assertThrows(CreatorException::class.java) {
            settlementService.confirm(settlement.id)
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    @Test
    fun `상태 전이 - CONFIRMED에서 confirm 재호출은 409`() {
        val settlement = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        settlementService.confirm(settlement.id)

        val e = assertThrows(CreatorException::class.java) {
            settlementService.confirm(settlement.id)
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    @Test
    fun `존재하지 않는 정산 id로 confirm-pay-get 하면 404`() {
        listOf<(String) -> Any>(
            { settlementService.confirm(it) },
            { settlementService.pay(it) },
            { settlementService.get(it) },
        ).forEach { action ->
            val e = assertThrows(CreatorException::class.java) { action("settlement-없음") }
            assertEquals(ErrorCode.NOT_FOUND, e.errorCode)
        }
    }

    @Test
    fun `스냅샷 불변성 - 정산 생성 후 판매가 추가되어도 스냅샷은 바뀌지 않는다`() {
        // creator-5 2025-06: 판매 150,000 → 정산 120,000 스냅샷 생성
        val settlement = settlementService.create(SettlementCreateParam("creator-5", "2025-06"))
        assertEquals(120000, settlement.settlementAmount)

        // 같은 달(course-9는 creator-5의 강의)에 판매 추가
        saleRecordService.register(
            SaleRecordCreateParam(
                courseId = "course-9",
                studentId = "student-99",
                amount = 100000,
                paidAt = LocalDateTime.of(2025, 6, 20, 12, 0, 0),
            ),
        )

        // 즉석 계산은 증가하지만 스냅샷은 그대로
        val recalculated = settlementService.getMonthly(MonthlySettlementParam("creator-5", "2025-06"))
        assertEquals(250000, recalculated.totalSales)

        val stored = settlementService.get(settlement.id)
        assertEquals(150000, stored.totalSales)
        assertEquals(120000, stored.settlementAmount)
    }

    @Test
    fun `목록 조회 - 크리에이터·월·상태 필터가 각각 적용된다`() {
        // V4 시드(settlement-1~5)가 이미 존재하므로 개수 비교 대신 포함·조건 만족으로 검증
        val s1 = settlementService.create(SettlementCreateParam("creator-1", "2025-03"))
        val s2 = settlementService.create(SettlementCreateParam("creator-2", "2025-01"))
        val s3 = settlementService.create(SettlementCreateParam("creator-4", "2025-06"))
        settlementService.confirm(s2.id)

        val byCreator = settlementService.list(SettlementListParam(creatorId = "creator-1"))
        assertTrue(byCreator.any { it.id == s1.id })
        assertTrue(byCreator.all { it.creatorId == "creator-1" })

        val byMonth = settlementService.list(SettlementListParam(month = "2025-01"))
        assertEquals(listOf(s2.id), byMonth.map { it.id })

        val byStatus = settlementService.list(SettlementListParam(status = SettlementStatus.CONFIRMED))
        assertTrue(byStatus.any { it.id == s2.id })
        assertTrue(byStatus.all { it.status == SettlementStatus.CONFIRMED })

        val all = settlementService.list(SettlementListParam())
        assertTrue(all.map { it.id }.containsAll(listOf(s1.id, s2.id, s3.id)))
    }

    @Test
    fun `시드 정산 - 스냅샷 금액이 판매·취소 데이터 계산과 일치한다`() {
        // settlement-2: creator-4 2025-03 PAID 시드 — V2 판매/취소로 계산한 값과 같아야 한다
        val seeded = settlementService.get("settlement-2")
        val calculated = settlementService.getMonthly(MonthlySettlementParam("creator-4", "2025-03"))

        assertEquals(SettlementStatus.PAID, seeded.status)
        assertEquals(calculated.totalSales, seeded.totalSales)
        assertEquals(calculated.totalRefund, seeded.totalRefund)
        assertEquals(calculated.netSales, seeded.netSales)
        assertEquals(calculated.fee, seeded.fee)
        assertEquals(calculated.settlementAmount, seeded.settlementAmount)
        assertEquals(calculated.saleCount, seeded.saleCount)
        assertEquals(calculated.cancelCount, seeded.cancelCount)
        assertNotNull(seeded.confirmedAt)
        assertNotNull(seeded.paidAt)
    }

    @Test
    fun `시드 정산 - 시드가 있는 크리에이터+월 재생성도 409`() {
        val e = assertThrows(CreatorException::class.java) {
            settlementService.create(SettlementCreateParam("creator-4", "2025-03"))
        }
        assertEquals(ErrorCode.CONFLICT, e.errorCode)
    }

    // ---- 수수료율 변경 이력 (V6 시드: 2024-01부터 20%, 2025-07부터 15%) ----

    @Test
    fun `수수료율 이력 - 2025-07 정산은 인하된 15%가 적용된다`() {
        // 시드에 2025-07 판매가 없으므로 테스트에서 등록
        saleRecordService.register(
            SaleRecordCreateParam(
                courseId = "course-1",
                studentId = "student-99",
                amount = 100000,
                paidAt = LocalDateTime.of(2025, 7, 10, 10, 0, 0),
            ),
        )

        val result = settlementService.getMonthly(MonthlySettlementParam("creator-1", "2025-07"))

        assertEquals(0, BigDecimal("0.15").compareTo(result.feeRate))
        assertEquals(100000, result.totalSales)
        assertEquals(15000, result.fee)
        assertEquals(85000, result.settlementAmount)
    }

    @Test
    fun `수수료율 이력 - 스냅샷은 대상 월에 유효했던 율을 저장한다`() {
        // creator-3 2025-07 빈 월 스냅샷 → 금액 0이어도 당시 율 15%가 기록된다
        val settlement = settlementService.create(SettlementCreateParam("creator-3", "2025-07"))

        assertEquals(0, BigDecimal("0.15").compareTo(settlement.feeRate))
        assertEquals(0, settlement.settlementAmount)
    }

    @Test
    fun `수수료율 이력 - 율 변경을 가로지르는 기간 집계는 월별 율로 계산된다`() {
        // creator-4: 6월(20%) 순 185,000 + 7월(15%) 순 100,000
        saleRecordService.register(
            SaleRecordCreateParam(
                courseId = "course-7",
                studentId = "student-98",
                amount = 100000,
                paidAt = LocalDateTime.of(2025, 7, 10, 10, 0, 0),
            ),
        )

        val result = settlementService.getSummary(
            PeriodSettlementParam(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 7, 31)),
        )

        assertNull(result.feeRate) // 구간 내 율이 20%·15% 둘 → 단일 율 아님
        val creator4 = result.creators.first { it.creatorId == "creator-4" }
        assertEquals(420000, creator4.totalSales)   // 320,000(6월) + 100,000(7월)
        assertEquals(135000, creator4.totalRefund)
        assertEquals(285000, creator4.netSales)
        assertEquals(52000, creator4.fee)           // 185,000×20% + 100,000×15% = 37,000 + 15,000
        assertEquals(233000, creator4.settlementAmount)
    }

    @Test
    fun `수수료율 이력 - 단일 율 구간의 기간 집계는 fee_rate가 채워진다`() {
        val result = settlementService.getSummary(
            PeriodSettlementParam(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)),
        )

        assertNotNull(result.feeRate)
        assertEquals(0, BigDecimal("0.20").compareTo(result.feeRate!!))
    }
}
