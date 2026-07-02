package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.Creator
import com.wonjiyap.creatorsettlementapi.domain.QCreator.creator
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorFetchParam
import org.springframework.data.jpa.repository.JpaRepository

interface CreatorRepository : JpaRepository<Creator, String>, CreatorCustomRepository

interface CreatorCustomRepository {
    fun fetchOne(param: CreatorFetchOneParam): Creator?
    fun fetch(param: CreatorFetchParam): List<Creator>
}

class CreatorCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CreatorCustomRepository {

    override fun fetchOne(param: CreatorFetchOneParam): Creator? =
        queryFactory
            .selectFrom(creator)
            .where(
                param.id?.let { creator.id.eq(it) },
            )
            .limit(1)
            .fetchOne()

    override fun fetch(param: CreatorFetchParam): List<Creator> =
        queryFactory
            .selectFrom(creator)
            .where(
                param.idArr?.let {
                    if (it.isEmpty()) return listOf()
                    creator.id.`in`(it)
                },
            )
            .fetch()
}
