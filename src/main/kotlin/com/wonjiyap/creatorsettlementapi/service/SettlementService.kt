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
     */
    @Transactional(readOnly = true)
    fun getSummary(param: PeriodSettlementParam): PeriodSettlementResult {
        if (param.from.isAfter(param.to)) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "시작일이 종료일보다 늦습니다: from ${param.from}, to ${param.to}")
        }
        val periodParam = PeriodParam(
            from = param.from.atStartOfDay(),
            to = param.to.atTime(23, 59, 59),
        )

        val salesByCreator = saleRecordRepository.fetchAmountCountByCreator(periodParam).associateBy { it.creatorId }
        val refundsByCreator =
            cancelRecordRepository.fetchAmountCountByCreator(periodParam).associateBy { it.creatorId }

        val creators = (salesByCreator.keys + refundsByCreator.keys).sorted().map { creatorId ->
            val sales = salesByCreator[creatorId]
            val refunds = refundsByCreator[creatorId]
            val totalSales = sales?.total ?: 0
            val totalRefund = refunds?.total ?: 0
            val netSales = totalSales - totalRefund
            val fee = feePolicy.calculateFee(netSales)
            CreatorSettlement(
                creatorId = creatorId,
                totalSales = totalSales,
                totalRefund = totalRefund,
                netSales = netSales,
                fee = fee,
                settlementAmount = netSales - fee,
                saleCount = sales?.count ?: 0,
                cancelCount = refunds?.count ?: 0,
            )
        }

        return PeriodSettlementResult(
            from = param.from,
            to = param.to,
            feeRate = feePolicy.feeRate,
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
     * 월 경계: 해당 월 1일 00:00:00 ~ 말일 23:59:59.
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
        val fee = feePolicy.calculateFee(netSales)

        return MonthlySettlementResult(
            creatorId = creatorId,
            month = yearMonth.toString(),
            totalSales = sales.total,
            totalRefund = refunds.total,
            netSales = netSales,
            feeRate = feePolicy.feeRate,
            fee = fee,
            settlementAmount = netSales - fee,
            saleCount = sales.count,
            cancelCount = refunds.count,
        )
    }

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
