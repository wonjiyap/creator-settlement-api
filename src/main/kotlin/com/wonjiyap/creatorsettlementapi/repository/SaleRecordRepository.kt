package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.QCourse.course
import com.wonjiyap.creatorsettlementapi.domain.QSaleRecord.saleRecord
import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import com.wonjiyap.creatorsettlementapi.repository.dto.AmountCount
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorAmountCount
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorPeriodParam
import com.wonjiyap.creatorsettlementapi.repository.dto.PeriodParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SaleRecordFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SaleRecordFetchParam
import org.springframework.data.jpa.repository.JpaRepository

interface SaleRecordRepository : JpaRepository<SaleRecord, String>, SaleRecordCustomRepository

interface SaleRecordCustomRepository {
    fun fetchOne(param: SaleRecordFetchOneParam): SaleRecord?

    /** 크리에이터의 기간 내 판매 금액 합계 + 건수 (paidAt 기준). */
    fun fetchAmountCount(param: CreatorPeriodParam): AmountCount

    /** 기간 내 크리에이터별 판매 금액 합계 + 건수 (paidAt 기준). */
    fun fetchAmountCountByCreator(param: PeriodParam): List<CreatorAmountCount>

    /** 크리에이터의 판매 목록 (기간 선택, paidAt 오름차순). */
    fun fetch(param: SaleRecordFetchParam): List<SaleRecord>
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

    override fun fetchAmountCountByCreator(param: PeriodParam): List<CreatorAmountCount> =
        queryFactory
            .select(
                Projections.constructor(
                    CreatorAmountCount::class.java,
                    course.creatorId,
                    saleRecord.amount.sum().coalesce(0L),
                    saleRecord.count(),
                ),
            )
            .from(saleRecord)
            .join(course).on(saleRecord.courseId.eq(course.id))
            .where(
                saleRecord.paidAt.goe(param.from),
                saleRecord.paidAt.loe(param.to),
            )
            .groupBy(course.creatorId)
            .fetch()

    override fun fetch(param: SaleRecordFetchParam): List<SaleRecord> {
        val query = queryFactory.selectFrom(saleRecord)
        if (param.creatorId != null) {
            query.join(course).on(saleRecord.courseId.eq(course.id))
        }
        return query
            .where(
                param.creatorId?.let { course.creatorId.eq(it) },
                param.from?.let { saleRecord.paidAt.goe(it) },
                param.to?.let { saleRecord.paidAt.loe(it) },
            )
            .orderBy(saleRecord.paidAt.asc())
            .fetch()
    }
}
