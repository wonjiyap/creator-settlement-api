package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.CancelRecord
import com.wonjiyap.creatorsettlementapi.domain.QCancelRecord.cancelRecord
import org.springframework.data.jpa.repository.JpaRepository

interface CancelRecordRepository : JpaRepository<CancelRecord, String>, CancelRecordCustomRepository

interface CancelRecordCustomRepository {
    fun sumRefundAmountBySaleRecordId(saleRecordId: String): Long
}

class CancelRecordCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CancelRecordCustomRepository {

    override fun sumRefundAmountBySaleRecordId(saleRecordId: String): Long =
        queryFactory
            .select(cancelRecord.refundAmount.sum())
            .from(cancelRecord)
            .where(cancelRecord.saleRecordId.eq(saleRecordId))
            .fetchOne() ?: 0L
}
