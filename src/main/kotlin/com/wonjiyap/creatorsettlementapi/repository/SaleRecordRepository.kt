package com.wonjiyap.creatorsettlementapi.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.wonjiyap.creatorsettlementapi.domain.QSaleRecord.saleRecord
import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import com.wonjiyap.creatorsettlementapi.repository.dto.SaleRecordFetchOneParam
import org.springframework.data.jpa.repository.JpaRepository

interface SaleRecordRepository : JpaRepository<SaleRecord, String>, SaleRecordCustomRepository

interface SaleRecordCustomRepository {
    fun fetchOne(param: SaleRecordFetchOneParam): SaleRecord?
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
}
