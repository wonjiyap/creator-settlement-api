package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.CancelRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.CreatorRepository
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorPeriodParam
import com.wonjiyap.creatorsettlementapi.repository.dto.PeriodParam
import com.wonjiyap.creatorsettlementapi.service.dto.CreatorSettlement
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementResult
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.format.DateTimeParseException

@Service
class SettlementService(
    private val saleRecordRepository: SaleRecordRepository,
    private val cancelRecordRepository: CancelRecordRepository,
    private val creatorRepository: CreatorRepository,
    private val feePolicy: FeePolicy,
) {

    /**
     * 크리에이터의 월별 정산을 계산한다.
     * 판매는 결제 완료 일시(paidAt), 취소는 취소 일시(canceledAt) 기준 / KST.
     * 월 경계: 해당 월 1일 00:00:00 ~ 말일 23:59:59.
     */
    @Transactional(readOnly = true)
    fun getMonthly(param: MonthlySettlementParam): MonthlySettlementResult {
        creatorRepository.fetchOne(CreatorFetchOneParam(id = param.creatorId))
            ?: throw CreatorException(ErrorCode.NOT_FOUND, "존재하지 않는 크리에이터입니다: ${param.creatorId}")

        val yearMonth = parseMonth(param.month)
        val periodParam = CreatorPeriodParam(
            creatorId = param.creatorId,
            from = yearMonth.atDay(1).atStartOfDay(),
            to = yearMonth.atEndOfMonth().atTime(23, 59, 59),
        )

        val sales = saleRecordRepository.fetchAmountCount(periodParam)
        val refunds = cancelRecordRepository.fetchAmountCount(periodParam)

        val netSales = sales.total - refunds.total
        val fee = feePolicy.calculateFee(netSales)

        return MonthlySettlementResult(
            creatorId = param.creatorId,
            month = param.month,
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
        val refundsByCreator = cancelRecordRepository.fetchAmountCountByCreator(periodParam).associateBy { it.creatorId }

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

    private fun parseMonth(month: String): YearMonth =
        try {
            YearMonth.parse(month)
        } catch (e: DateTimeParseException) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "잘못된 연월 형식입니다: $month (예: 2025-03)")
        }
}
