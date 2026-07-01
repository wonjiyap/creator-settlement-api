package com.wonjiyap.creatorsettlementapi.service

import com.wonjiyap.creatorsettlementapi.exception.CreatorException
import com.wonjiyap.creatorsettlementapi.repository.CancelRecordRepository
import com.wonjiyap.creatorsettlementapi.service.dto.CancelRecordCreateParam
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
class CancelRecordServiceTest(
    @Autowired val cancelRecordService: CancelRecordService,
    @Autowired val cancelRecordRepository: CancelRecordRepository,
) {

    @Test
    fun `취소를 등록하면 저장되고 id가 부여된다`() {
        val before = cancelRecordRepository.count()

        // sale-1: 50,000, 기존 취소 없음
        val saved = cancelRecordService.register(
            CancelRecordCreateParam(
                saleRecordId = "sale-1",
                refundAmount = 30000,
                canceledAt = LocalDateTime.of(2025, 7, 1, 10, 0),
            ),
        )

        assertTrue(saved.id.isNotBlank())
        assertEquals(before + 1, cancelRecordRepository.count())
        assertTrue(cancelRecordRepository.findById(saved.id).isPresent)
    }

    @Test
    fun `존재하지 않는 판매면 예외가 발생한다`() {
        assertThrows(CreatorException::class.java) {
            cancelRecordService.register(
                CancelRecordCreateParam(
                    saleRecordId = "sale-없음",
                    refundAmount = 10000,
                    canceledAt = LocalDateTime.of(2025, 7, 1, 10, 0),
                ),
            )
        }
    }

    @Test
    fun `환불 누적액이 원결제 금액을 초과하면 예외가 발생한다`() {
        // sale-4: 80,000, 이미 cancel-2로 30,000 환불됨 → 60,000 추가 시 누적 90,000 > 80,000
        assertThrows(CreatorException::class.java) {
            cancelRecordService.register(
                CancelRecordCreateParam(
                    saleRecordId = "sale-4",
                    refundAmount = 60000,
                    canceledAt = LocalDateTime.of(2025, 7, 1, 10, 0),
                ),
            )
        }
    }

    @Test
    fun `기환불 없이 환불 요청이 결제 금액을 초과하면 예외가 발생한다`() {
        // sale-1: 50,000, 기존 취소 없음 → 60,000 요청 시 잔액 50,000 초과
        assertThrows(CreatorException::class.java) {
            cancelRecordService.register(
                CancelRecordCreateParam(
                    saleRecordId = "sale-1",
                    refundAmount = 60000,
                    canceledAt = LocalDateTime.of(2025, 7, 1, 10, 0),
                ),
            )
        }
    }
}
