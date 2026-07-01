package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.domain.CancelRecord
import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.CancelRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.SaleRecordFetchOneParam
import com.wonjiyap.creatorsettlementapi.service.dto.CancelRecordCreateParam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CancelRecordService(
    private val cancelRecordRepository: CancelRecordRepository,
    private val saleRecordRepository: SaleRecordRepository,
) {

    @Transactional
    fun register(param: CancelRecordCreateParam): CancelRecord {
        val sale = saleRecordRepository.fetchOne(
            SaleRecordFetchOneParam(
                id = param.saleRecordId,
                )
        ) ?: throw CreatorException(ErrorCode.NOT_FOUND, "존재하지 않는 판매 내역입니다: ${param.saleRecordId}")

        val alreadyRefunded = cancelRecordRepository.sumRefundAmountBySaleRecordId(param.saleRecordId)
        val refundableAmount = sale.amount - alreadyRefunded
        if (param.refundAmount > refundableAmount) {
            throw CreatorException(
                ErrorCode.BAD_REQUEST,
                "환불 요청 금액이 환불 가능 잔액을 초과합니다: 잔액 $refundableAmount, 요청 ${param.refundAmount}",
            )
        }

        val cancel = CancelRecord(
            id = "cancel-${UUID.randomUUID()}",
            saleRecordId = param.saleRecordId,
            refundAmount = param.refundAmount,
            canceledAt = param.canceledAt,
        )
        return cancelRecordRepository.save(cancel)
    }
}
