package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.domain.SaleRecord
import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.exception.ErrorCode
import com.wonjiyap.creatorsettlementapi.repository.CourseRepository
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.repository.dto.CourseFetchOneParam
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SaleRecordService(
    private val saleRecordRepository: SaleRecordRepository,
    private val courseRepository: CourseRepository,
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
}
