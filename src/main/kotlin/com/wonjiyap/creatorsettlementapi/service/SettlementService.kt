package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.CancelRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorPeriodParam
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.format.DateTimeParseException

@Service
class SettlementService(
    private val saleRecordRepository: SaleRecordRepository,
    private val cancelRecordRepository: CancelRecordRepository,
    private val feePolicy: FeePolicy,
) {

    /**
     * 크리에이터의 월별 정산을 계산한다.
     * 판매는 결제 완료 일시(paidAt), 취소는 취소 일시(canceledAt) 기준 / KST.
     * 월 경계: 해당 월 1일 00:00:00 ~ 말일 23:59:59.
     */
    @Transactional(readOnly = true)
    fun getMonthly(param: MonthlySettlementParam): MonthlySettlementResult {
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

    private fun parseMonth(month: String): YearMonth =
        try {
            YearMonth.parse(month)
        } catch (e: DateTimeParseException) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "잘못된 연월 형식입니다: $month (예: 2025-03)")
        }
}
