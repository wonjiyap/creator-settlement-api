package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.QCourse.course
import com.wonjiyap.creatorsettlementapi.domain.QSaleRecord.saleRecord
import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import com.wonjiyap.creatorsettlementapi.repository.dto.AmountCount
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorPeriodParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SaleRecordFetchOneParam
import org.springframework.data.jpa.repository.JpaRepository

interface SaleRecordRepository : JpaRepository<SaleRecord, String>, SaleRecordCustomRepository

interface SaleRecordCustomRepository {
    fun fetchOne(param: SaleRecordFetchOneParam): SaleRecord?

    /** 크리에이터의 기간 내 판매 금액 합계 + 건수 (paidAt 기준). */
    fun fetchAmountCount(param: CreatorPeriodParam): AmountCount
}

class SaleRecordCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SaleRecordCustomRepository {

    override fun fetchOne(param: SaleRecordFetchOneParam): SaleRecord? =
        queryFactory
            .selectFrom(saleRecord)
            .where(
                param.id?.let { saleRecord.id.eq(it) },
            )
            .limit(1)
            .fetchOne()

    override fun fetchAmountCount(param: CreatorPeriodParam): AmountCount =
        queryFactory
            .select(
                Projections.constructor(
                    AmountCount::class.java,
                    saleRecord.amount.sum().coalesce(0L),
                    saleRecord.count(),
                ),
            )
            .from(saleRecord)
            .join(course).on(saleRecord.courseId.eq(course.id))
            .where(
                course.creatorId.eq(param.creatorId),
                saleRecord.paidAt.goe(param.from),
                saleRecord.paidAt.loe(param.to),
            )
            .fetchOne()!!
}
