package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.FeeRateHistory
import com.wonjiyap.creatorsettlementapi.domain.QFeeRateHistory.feeRateHistory
import com.wonjiyap.creatorsettlementapi.repository.dto.EffectiveFeeRateParam
import com.wonjiyap.creatorsettlementapi.repository.dto.FeeRateHistoryFetchOneParam
import org.springframework.data.jpa.repository.JpaRepository

interface FeeRateHistoryRepository : JpaRepository<FeeRateHistory, String>, FeeRateHistoryCustomRepository

interface FeeRateHistoryCustomRepository {
    fun fetchOne(param: FeeRateHistoryFetchOneParam): FeeRateHistory?

    /** 해당 연월에 유효한 이력: effectiveMonth <= month 중 가장 최신. 없으면 null. */
    fun fetchEffective(param: EffectiveFeeRateParam): FeeRateHistory?

    /** 이력 전체 (effectiveMonth 내림차순). */
    fun fetch(): List<FeeRateHistory>
}

class FeeRateHistoryCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : FeeRateHistoryCustomRepository {

    override fun fetchOne(param: FeeRateHistoryFetchOneParam): FeeRateHistory? =
        queryFactory
            .selectFrom(feeRateHistory)
            .where(
                param.id?.let { feeRateHistory.id.eq(it) },
                param.effectiveMonth?.let { feeRateHistory.effectiveMonth.eq(it) },
            )
            .limit(1)
            .fetchOne()

    override fun fetchEffective(param: EffectiveFeeRateParam): FeeRateHistory? =
        queryFactory
            .selectFrom(feeRateHistory)
            .where(feeRateHistory.effectiveMonth.loe(param.month))
            .orderBy(feeRateHistory.effectiveMonth.desc())
            .limit(1)
            .fetchOne()

    override fun fetch(): List<FeeRateHistory> =
        queryFactory
            .selectFrom(feeRateHistory)
            .orderBy(feeRateHistory.effectiveMonth.desc())
            .fetch()
}
