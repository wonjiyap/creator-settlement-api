package com.wonjiyap.creatorsettlementapi.controller

import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementCreateRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementDetailResponse
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementDownloadRequest
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementResponse
import com.wonjiyap.creatorsettlementapi.controller.dto.SettlementSummaryResponse
import com.wonjiyap.creatorsettlementapi.domain.SettlementStatus
import com.wonjiyap.creatorsettlementapi.service.SettlementService
import com.wonjiyap.creatorsettlementapi.service.dto.MonthlySettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.PeriodSettlementParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementCreateParam
import com.wonjiyap.creatorsettlementapi.service.dto.SettlementListParam
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Tag(name = "Settlement", description = "정산")
@RestController
@RequestMapping("/api/settlement")
class SettlementController(
    private val settlementService: SettlementService,
) {

    @GetMapping("/monthly")
    fun getMonthly(
        @RequestParam("creator_id") creatorId: String,
        @RequestParam month: String,
    ) = SettlementResponse.from(
        settlementService.getMonthly(
            MonthlySettlementParam(creatorId = creatorId, month = month),
        ),
    )

    @GetMapping("/summary")
    fun getSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ) = SettlementSummaryResponse.from(
        settlementService.getSummary(
            PeriodSettlementParam(from = from, to = to),
        ),
    )

    /** 정산 스냅샷 생성(PENDING). 동일 크리에이터+월 중복 시 409. */
    @PostMapping
    fun create(
        @Valid @RequestBody request: SettlementCreateRequest,
    ) = SettlementDetailResponse.from(
        settlementService.create(
            SettlementCreateParam(
                creatorId = request.creatorId,
                month = request.month,
            ),
        ),
    )

    /** PENDING → CONFIRMED. */
    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: String,
    ) = SettlementDetailResponse.from(settlementService.confirm(id))

    /** CONFIRMED → PAID. */
    @PostMapping("/{id}/pay")
    fun pay(
        @PathVariable id: String,
    ) = SettlementDetailResponse.from(settlementService.pay(id))

    @GetMapping("/list")
    fun list(
        @RequestParam("creator_id", required = false) creatorId: String?,
        @RequestParam(required = false) month: String?,
        @RequestParam(required = false) status: SettlementStatus?,
    ) = settlementService.list(
        SettlementListParam(
            creatorId = creatorId,
            month = month,
            status = status,
        ),
    ).map { SettlementDetailResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ) = SettlementDetailResponse.from(settlementService.get(id))

    /** 정산 목록 CSV 다운로드 (list와 동일 필터, 본문 생략 시 전체). UTF-8 BOM 포함 — 엑셀에서 한글 깨짐 없이 열린다. */
    @PostMapping("/download/csv")
    fun downloadCsv(
        @RequestBody(required = false) request: SettlementDownloadRequest?,
    ): ResponseEntity<ByteArray> {
        val rows = settlementService.list(
            SettlementListParam(
                creatorId = request?.creatorId,
                month = request?.month,
                status = request?.status,
            ),
        ).map { SettlementDetailResponse.from(it) }

        // 쉼표/따옴표/개행이 들어간 값은 따옴표로 감싸고 내부 따옴표는 이중화
        fun cell(value: Any?): String {
            val text = value?.toString() ?: ""
            return if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"${text.replace("\"", "\"\"")}\""
            } else {
                text
            }
        }

        val csv = StringBuilder(UTF8_BOM)
        csv.appendLine(exportColumns.joinToString(",") { it.first })
        rows.forEach { row ->
            csv.appendLine(exportColumns.joinToString(",") { (_, extract) -> cell(extract(row)) })
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("settlements.csv").build().toString(),
            )
            .body(csv.toString().toByteArray(Charsets.UTF_8))
    }

    /** 정산 목록 엑셀(xlsx) 다운로드 (list와 동일 필터, 본문 생략 시 전체). 금액·건수는 숫자 셀로 저장한다. */
    @PostMapping("/download/excel")
    fun downloadExcel(
        @RequestBody(required = false) request: SettlementDownloadRequest?,
    ): ResponseEntity<ByteArray> {
        val rows = settlementService.list(
            SettlementListParam(
                creatorId = request?.creatorId,
                month = request?.month,
                status = request?.status,
            ),
        ).map { SettlementDetailResponse.from(it) }

        val excel = XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("settlements")
            val header = sheet.createRow(0)
            exportColumns.forEachIndexed { index, (name, _) -> header.createCell(index).setCellValue(name) }
            rows.forEachIndexed { rowIndex, row ->
                val sheetRow = sheet.createRow(rowIndex + 1)
                exportColumns.forEachIndexed { index, (_, extract) ->
                    val cell = sheetRow.createCell(index)
                    when (val value = extract(row)) {
                        null -> cell.setBlank()
                        is Long -> cell.setCellValue(value.toDouble())
                        is BigDecimal -> cell.setCellValue(value.toDouble())
                        else -> cell.setCellValue(value.toString())
                    }
                }
            }
            ByteArrayOutputStream().also { workbook.write(it) }.toByteArray()
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("settlements.xlsx").build().toString(),
            )
            .body(excel)
    }

    /** 내보내기 컬럼 정의: 한글 헤더 + 값 추출. CSV/엑셀의 컬럼이 어긋나지 않도록 하나만 둔다. */
    private val exportColumns: List<Pair<String, (SettlementDetailResponse) -> Any?>> = listOf(
        "정산 ID" to { it.id },
        "크리에이터 ID" to { it.creatorId },
        "정산 월" to { it.month },
        "상태" to { it.status.name },
        "총 판매" to { it.totalSales },
        "총 환불" to { it.totalRefund },
        "순 판매" to { it.netSales },
        "수수료율" to { it.feeRate },
        "수수료" to { it.fee },
        "정산 예정 금액" to { it.settlementAmount },
        "판매 건수" to { it.saleCount },
        "취소 건수" to { it.cancelCount },
        "생성 일시" to { it.createdAt.format(EXPORT_DATE_TIME) },
        "확정 일시" to { it.confirmedAt?.format(EXPORT_DATE_TIME) },
        "지급 일시" to { it.paidAt?.format(EXPORT_DATE_TIME) },
    )

    companion object {
        private const val UTF8_BOM = "\uFEFF"
        private const val EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        private val EXPORT_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
