package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import com.wonjiyap.creatorsettlementapi.domain.fromCourses
import com.wonjiyap.creatorsettlementapi.domain.fromCreators
import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.CourseRepository
import com.wonjiyap.creatorsettlementapi.repository.CreatorRepository
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.CourseFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CourseFetchParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorFetchOneParam
import com.wonjiyap.creatorsettlementapi.repository.dto.CreatorFetchParam
import com.wonjiyap.creatorsettlementapi.repository.dto.SaleRecordFetchParam
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordListParam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SaleRecordService(
    private val saleRecordRepository: SaleRecordRepository,
    private val courseRepository: CourseRepository,
    private val creatorRepository: CreatorRepository,
) {

    @Transactional
    fun register(param: SaleRecordCreateParam): SaleRecord {
        courseRepository.fetchOne(
            CourseFetchOneParam(
                id = param.courseId,
                )
        ) ?: throw CreatorException(ErrorCode.NOT_FOUND, "존재하지 않는 강의입니다: ${param.courseId}")

        val sale = SaleRecord(
            id = "sale-${UUID.randomUUID()}",
            courseId = param.courseId,
            studentId = param.studentId,
            amount = param.amount,
            paidAt = param.paidAt,
        )
        return saleRecordRepository.save(sale)
    }

    @Transactional(readOnly = true)
    fun list(param: SaleRecordListParam): List<SaleRecord> {
        param.creatorId?.let { creatorId ->
            creatorRepository.fetchOne(CreatorFetchOneParam(id = creatorId))
                ?: throw CreatorException(ErrorCode.NOT_FOUND, "존재하지 않는 크리에이터입니다: $creatorId")
        }

        if (param.from != null && param.to != null && param.from.isAfter(param.to)) {
            throw CreatorException(ErrorCode.BAD_REQUEST, "시작일이 종료일보다 늦습니다: from ${param.from}, to ${param.to}")
        }

        val list =  saleRecordRepository.fetch(
            SaleRecordFetchParam(
                creatorId = param.creatorId,
                from = param.from?.atStartOfDay(),
                to = param.to?.atTime(23, 59, 59),
            ),
        )

        list.fromCourses(
            courseRepository.fetch(
                CourseFetchParam(
                    idArr = list.map { it.courseId }.distinct(),
                )
            ).apply {
                fromCreators(
                    creatorRepository.fetch(
                        CreatorFetchParam(
                            idArr = this.map { it.creatorId }.distinct(),
                        )
                    )
                )
            }
        )

        return list
    }
}
