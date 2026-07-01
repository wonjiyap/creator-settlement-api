package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class SaleServiceTest(
    @Autowired val saleService: SaleRecordService,
    @Autowired val saleRecordRepository: SaleRecordRepository,
) {

    @Test
    fun `판매를 등록하면 저장되고 id가 부여된다`() {
        val before = saleRecordRepository.count()

        val saved = saleService.register(
            SaleRecordCreateParam(
                courseId = "course-1",
                studentId = "student-100",
                amount = 50000,
                paidAt = LocalDateTime.of(2025, 7, 1, 10, 0),
            ),
        )

        assertTrue(saved.id.isNotBlank())
        assertEquals(before + 1, saleRecordRepository.count())
        assertTrue(saleRecordRepository.findById(saved.id).isPresent)
    }

    @Test
    fun `존재하지 않는 강의면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            saleService.register(
                SaleRecordCreateParam(
                    courseId = "course-없음",
                    studentId = "student-100",
                    amount = 50000,
                    paidAt = LocalDateTime.of(2025, 7, 1, 10, 0),
                ),
            )
        }
    }
}
