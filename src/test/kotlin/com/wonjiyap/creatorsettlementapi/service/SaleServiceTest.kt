package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.repository.SaleRecordRepository
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SaleRecordListParam
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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

    @Test
    fun `목록 조회 - 조건 없이 전체 판매`() {
        // 시드 전체 판매 32건
        val result = saleService.list(SaleRecordListParam())

        assertEquals(32, result.size)
    }

    @Test
    fun `목록 조회 - 크리에이터 전체 판매`() {
        // creator-1 판매: sale-1~4(3월) + sale-111,112(4월) + sale-113,114(5월) + sale-115(2024-12) = 9건
        val result = saleService.list(SaleRecordListParam(creatorId = "creator-1"))

        assertEquals(9, result.size)
    }

    @Test
    fun `목록 조회 - 크리에이터 + 기간 필터`() {
        // creator-1 2025-03: sale-1~4 = 4건
        val result = saleService.list(
            SaleRecordListParam(
                creatorId = "creator-1",
                from = LocalDate.of(2025, 3, 1),
                to = LocalDate.of(2025, 3, 31),
            ),
        )

        assertEquals(4, result.size)
        assertTrue(result.all { it.paidAt.year == 2025 && it.paidAt.monthValue == 3 })
    }

    @Test
    fun `목록 조회 - 존재하지 않는 크리에이터면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            saleService.list(SaleRecordListParam(creatorId = "creator-없음"))
        }
    }

    @Test
    fun `목록 조회 - 시작일이 종료일보다 늦으면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            saleService.list(
                SaleRecordListParam(
                    creatorId = "creator-1",
                    from = LocalDate.of(2025, 3, 31),
                    to = LocalDate.of(2025, 3, 1),
                ),
            )
        }
    }
}
