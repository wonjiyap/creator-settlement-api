package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.QSettlement.settlement
import com.wonjiyap.creatorsettlementapi.domain.Settlement
import com.wonjiyap.creatorsettlementapi.repository.dto.SettlementFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SettlementFetchParam
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, String>, SettlementCustomRepository

interface SettlementCustomRepository {
    fun fetchOne(param: SettlementFetchOneParam): Settlement?

    /** 정산 목록 (creatorId/period/status 선택 필터, createdAt 내림차순). */
    fun fetch(param: SettlementFetchParam): List<Settlement>
}

class SettlementCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : SettlementCustomRepository {

    override fun fetchOne(param: SettlementFetchOneParam): Settlement? =
        queryFactory
            .selectFrom(settlement)
            .where(
                param.id?.let { settlement.id.eq(it) },
                param.creatorId?.let { settlement.creatorId.eq(it) },
                param.period?.let { settlement.period.eq(it) },
            )
            .limit(1)
            .fetchOne()

    override fun fetch(param: SettlementFetchParam): List<Settlement> =
        queryFactory
            .selectFrom(settlement)
            .where(
                param.creatorId?.let { settlement.creatorId.eq(it) },
                param.period?.let { settlement.period.eq(it) },
                param.status?.let { settlement.status.eq(it) },
            )
            .orderBy(settlement.createdAt.desc())
            .fetch()
}
