package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.CancelRecord
import com.wonjiyap.creatorsettlementapi.domain.QCancelRecord.cancelRecord
import com.wonjiyap.creatorsettlementapi.domain.QCourse.course
import com.wonjiyap.creatorsettlementapi.domain.QSaleRecord.saleRecord
import com.wonjiyap.creatorsettlementapi.repository.dto.AmountCount
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorPeriodParam
import org.springframework.data.jpa.repository.JpaRepository

interface CancelRecordRepository : JpaRepository<CancelRecord, String>, CancelRecordCustomRepository

interface CancelRecordCustomRepository {
    fun sumRefundAmountBySaleRecordId(saleRecordId: String): Long

    /** 크리에이터의 기간 내 환불 금액 합계 + 건수 (canceledAt 기준). */
    fun fetchAmountCount(param: CreatorPeriodParam): AmountCount
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

    override fun fetchAmountCount(param: CreatorPeriodParam): AmountCount =
        queryFactory
            .select(
                Projections.constructor(
                    AmountCount::class.java,
                    cancelRecord.refundAmount.sum().coalesce(0L),
                    cancelRecord.count(),
                ),
            )
            .from(cancelRecord)
            .join(saleRecord).on(cancelRecord.saleRecordId.eq(saleRecord.id))
            .join(course).on(saleRecord.courseId.eq(course.id))
            .where(
                course.creatorId.eq(param.creatorId),
                cancelRecord.canceledAt.goe(param.from),
                cancelRecord.canceledAt.loe(param.to),
            )
            .fetchOne()!!
}
