package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.domain.Settlement
import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.CancelRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.CreatorRepository
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.SettlementRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorPeriodParam
import com.wonjiyap.creatorsettlementapi.repository.dto.PeriodParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SettlementFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SettlementFetchParam
import com.wonjiyap.creatorsettlementapi.service.dto.CreatorSettlement
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementResult
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementResult
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementListParam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.UUID

@Service
class SettlementService(
    private val saleRecordRepository: SaleRecordRepository,
    private val cancelRecordRepository: CancelRecordRepository,
    private val creatorRepository: CreatorRepository,
    private val settlementRepository: SettlementRepository,
    private val feePolicy: FeePolicy,
) {

    /** 크리에이터의 월별 정산을 계산한다. 계산 규칙은 [calculateMonthly] 참조. */
    @Transactional(readOnly = true)
    fun getMonthly(param: MonthlySettlementParam): MonthlySettlementResult =
        calculateMonthly(param.creatorId, parseMonth(param.month))

    /**
     * 운영자용: 기간(from ~ to, KST) 내 크리에이터별 정산 목록과 전체 정산 합계.
     * 판매·환불이 하나라도 있는 크리에이터만 포함한다.
     * 수수료율이 월 단위로 변할 수 있으므로 구간을 월별로 분해해 각 월의 율로 수수료를 계산·합산한다.
     */
    @Transactional(readOnly = true)
    fun getSummary(param: PeriodSettlementParam): PeriodSettlementResult {
        if (param.from.isAfter(param.to)) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "시작일이 종료일보다 늦습니다: from ${param.from}, to ${param.to}")
        }

        val segments = monthlySegments(param.from, param.to)
        val rateByMonth = segments.associate { (yearMonth, _) -> yearMonth to feePolicy.rateFor(yearMonth) }

        val totals = sortedMapOf<String, CreatorTotals>()
        // 같은 율이 적용된 순매출끼리 묶어 수수료를 계산한다 → 율이 하나인 구간은 분해 전과 결과 동일
        val netByCreatorAndRate = mutableMapOf<Pair<String, BigDecimal>, Long>()

        for ((yearMonth, periodParam) in segments) {
            val rate = rateByMonth.getValue(yearMonth)
            val sales = saleRecordRepository.fetchAmountCountByCreator(periodParam).associateBy { it.creatorId }
            val refunds = cancelRecordRepository.fetchAmountCountByCreator(periodParam).associateBy { it.creatorId }

            for (creatorId in sales.keys + refunds.keys) {
                val monthSales = sales[creatorId]?.total ?: 0
                val monthRefund = refunds[creatorId]?.total ?: 0
                val acc = totals.getOrPut(creatorId) { CreatorTotals() }
                acc.totalSales += monthSales
                acc.totalRefund += monthRefund
                acc.saleCount += sales[creatorId]?.count ?: 0
                acc.cancelCount += refunds[creatorId]?.count ?: 0
                netByCreatorAndRate.merge(creatorId to rate, monthSales - monthRefund, Long::plus)
            }
        }

        val creators = totals.map { (creatorId, acc) ->
            val netSales = acc.totalSales - acc.totalRefund
            val fee = netByCreatorAndRate.entries
                .filter { it.key.first == creatorId }
                .sumOf { (key, net) -> feePolicy.calculateFee(net, key.second) }
            CreatorSettlement(
                creatorId = creatorId,
                totalSales = acc.totalSales,
                totalRefund = acc.totalRefund,
                netSales = netSales,
                fee = fee,
                settlementAmount = netSales - fee,
                saleCount = acc.saleCount,
                cancelCount = acc.cancelCount,
            )
        }

        return PeriodSettlementResult(
            from = param.from,
            to = param.to,
            feeRate = rateByMonth.values.distinct().singleOrNull(),
            totalSettlementAmount = creators.sumOf { it.settlementAmount },
            creators = creators,
        )
    }

    /**
     * 정산 스냅샷 생성(PENDING). 금액은 생성 시점의 월별 정산 계산으로 고정된다.
     * 동일 크리에이터 + 월에 정산이 이미 존재하면 상태와 무관하게 409.
     * (사전 조회 + settlement(creator_id, period) 유니크 제약 이중 방어)
     */
    @Transactional
    fun create(param: SettlementCreateParam): Settlement {
        val yearMonth = parseMonth(param.month)
        val period = yearMonth.toString()

        settlementRepository.fetchOne(
            SettlementFetchOneParam(
                creatorId = param.creatorId,
                period = period,
            )
        )?.let {
            throw CreatorException(
                ErrorCode.CONFLICT,
                "이미 해당 월의 정산이 존재합니다: ${param.creatorId} / $period (상태: ${it.status})",
            )
        }

        val result = calculateMonthly(param.creatorId, yearMonth)

        return settlementRepository.save(
            Settlement(
                id = "settlement-${UUID.randomUUID()}",
                creatorId = param.creatorId,
                period = period,
                totalSales = result.totalSales,
                totalRefund = result.totalRefund,
                netSales = result.netSales,
                feeRate = result.feeRate,
                fee = result.fee,
                settlementAmount = result.settlementAmount,
                saleCount = result.saleCount,
                cancelCount = result.cancelCount,
                createdAt = LocalDateTime.now(),
            ),
        )
    }

    /** PENDING → CONFIRMED 전이. 그 외 상태에서는 409. */
    @Transactional
    fun confirm(id: String): Settlement {
        val settlement = getOrThrow(id)
        settlement.confirm(LocalDateTime.now())
        return settlementRepository.save(settlement)
    }

    /** CONFIRMED → PAID 전이. 그 외 상태에서는 409. */
    @Transactional
    fun pay(id: String): Settlement {
        val settlement = getOrThrow(id)
        settlement.pay(LocalDateTime.now())
        return settlementRepository.save(settlement)
    }

    @Transactional(readOnly = true)
    fun get(id: String): Settlement = getOrThrow(id)

    @Transactional(readOnly = true)
    fun list(param: SettlementListParam): List<Settlement> =
        settlementRepository.fetch(
            SettlementFetchParam(
                creatorId = param.creatorId,
                period = param.month?.let { parseMonth(it).toString() },
                status = param.status,
            ),
        )

    /**
     * 크리에이터의 월별 정산 계산 (getMonthly 조회와 create 스냅샷이 공유).
     * 판매는 결제 완료 일시(paidAt), 취소는 취소 일시(canceledAt) 기준 / KST.
     * 월 경계: 해당 월 1일 00:00:00 ~ 말일 23:59:59. 수수료율은 해당 연월에 유효한 율.
     */
    private fun calculateMonthly(creatorId: String, yearMonth: YearMonth): MonthlySettlementResult {
        creatorRepository.fetchOne(CreatorFetchOneParam(id = creatorId))
            ?: throw CreatorException(ErrorCode.NOT_FOUND, "존재하지 않는 크리에이터입니다: $creatorId")

        val periodParam = CreatorPeriodParam(
            creatorId = creatorId,
            from = yearMonth.atDay(1).atStartOfDay(),
            to = yearMonth.atEndOfMonth().atTime(23, 59, 59),
        )

        val sales = saleRecordRepository.fetchAmountCount(periodParam)
        val refunds = cancelRecordRepository.fetchAmountCount(periodParam)

        val netSales = sales.total - refunds.total
        val feeRate = feePolicy.rateFor(yearMonth)
        val fee = feePolicy.calculateFee(netSales, feeRate)

        return MonthlySettlementResult(
            creatorId = creatorId,
            month = yearMonth.toString(),
            totalSales = sales.total,
            totalRefund = refunds.total,
            netSales = netSales,
            feeRate = feeRate,
            fee = fee,
            settlementAmount = netSales - fee,
            saleCount = sales.count,
            cancelCount = refunds.count,
        )
    }

    /** 구간을 월 단위 세그먼트로 분해한다 (부분 월 포함, 경계 시각은 기존 규칙 그대로). */
    private fun monthlySegments(from: LocalDate, to: LocalDate): List<Pair<YearMonth, PeriodParam>> {
        val segments = mutableListOf<Pair<YearMonth, PeriodParam>>()
        var yearMonth = YearMonth.from(from)
        val lastMonth = YearMonth.from(to)
        while (yearMonth <= lastMonth) {
            val segmentFrom = maxOf(from, yearMonth.atDay(1))
            val segmentTo = minOf(to, yearMonth.atEndOfMonth())
            segments += yearMonth to PeriodParam(
                from = segmentFrom.atStartOfDay(),
                to = segmentTo.atTime(23, 59, 59),
            )
            yearMonth = yearMonth.plusMonths(1)
        }
        return segments
    }

    /** getSummary의 크리에이터별 누적 집계용. */
    private class CreatorTotals(
        var totalSales: Long = 0,
        var totalRefund: Long = 0,
        var saleCount: Long = 0,
        var cancelCount: Long = 0,
    )

    private fun getOrThrow(id: String): Settlement =
        settlementRepository.fetchOne(
            SettlementFetchOneParam(
                id = id,
                )
        ) ?: throw CreatorException(ErrorCode.NOT_FOUND, "존재하지 않는 정산입니다: $id")

    private fun parseMonth(month: String): YearMonth =
        try {
            YearMonth.parse(month)
        } catch (e: DateTimeParseException) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "잘못된 연월 형식입니다: $month (예: 2025-03)")
        }
}
