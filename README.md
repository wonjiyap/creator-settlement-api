## 프로젝트 개요
크리에이터 정산(Creator Settlement) API입니다.

크리에이터는 강의를 개설해 수강생에게 판매하고, 플랫폼은 판매 금액에서 수수료(20%)를 제한 뒤 크리에이터에게 정산합니다. 수강 취소가 발생하면 이미 계산된 정산 금액에서 환불분이 차감됩니다.

주요 기능은 다음과 같습니다.

- **판매/취소 내역 관리**: 판매 내역 등록, 취소(환불) 내역 등록, 크리에이터별·기간별 판매 내역 조회
- **크리에이터별 월별 정산 계산**: 총 판매 / 환불 / 순 판매 / 수수료 / 정산 예정 금액 및 판매·취소 건수 산출
- **운영자용 정산 집계**: 특정 기간 내 전체 크리에이터의 정산 현황 및 합계 조회
- **정산 상태 관리 & 중복 정산 방지**: 크리에이터+월 단위 정산 스냅샷 생성(PENDING) 및 상태 전이(PENDING → CONFIRMED → PAID), 동일 크리에이터+월 중복 정산 차단(409)
- **수수료율 변경 이력 관리**: 월 단위 수수료율 변경 등록·이력 조회, 정산 계산 시 해당 월에 유효했던 율 적용(과거 정산은 당시 수수료율)
- **정산 내역 CSV/엑셀 다운로드**: 정산 목록을 동일 필터로 CSV(UTF-8 BOM)·엑셀(xlsx) 파일로 내보내기

정산 기간 기준은 **결제 완료 일시(취소는 취소 일시) 기준 / KST**이며, 월 경계는 해당 월 1일 00:00:00 ~ 말일 23:59:59 로 처리합니다.

## 기술 스택
| 구분 | 사용 기술 |
|------|----|
| Language | Kotlin 2.3.21 (JVM 17) |
| Framework | Spring Boot 3.5.3 (Spring Web MVC, Spring Data JPA, Validation) |
| Query | QueryDSL |
| Database | H2 |
| DB Migration | Flyway |
| API Docs | Swagger UI |
| Excel Export | Apache POI (poi-ooxml) |

## 실행 방법
**요구 사항**: JDK 17 이상 (별도 DB 설치 불필요 — H2 사용)

```bash
# 애플리케이션 실행
./gradlew bootRun
```

실행 후 접속 정보:

| 항목 | URL |
|------|-----|
| API 서버 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/api-docs |
| H2 Console | http://localhost:8080/h2-console |

## API 목록 및 예시

### 판매 내역 등록 — `POST /api/sale-record`
요청 본문의 `course_id`는 존재하는 강의여야 하며, `amount`는 0보다 커야 한다. 서버가 `id`를 생성한다.
요청·응답 본문 모두 **snake_case**다(Jackson 전역 설정).
`paid_at`은 **`yyyy-MM-dd HH:mm:ss`(KST) 형식만 허용**한다 — ISO-8601(`2025-03-05T10:00:00+09:00`) 등 다른 형식은 `400`.

요청
```http
POST /api/sale-record
Content-Type: application/json

{
  "course_id": "course-1",
  "student_id": "student-100",
  "amount": 50000,
  "paid_at": "2025-07-01 10:00:00"
}
```

응답 `200 OK`
```json
{
  "id": "sale-3f2c…",
  "course_id": "course-1",
  "student_id": "student-100",
  "amount": 50000,
  "paid_at": "2025-07-01 10:00:00"
}
```

**에러 응답** — 전역 예외 처리(`@RestControllerAdvice`)가 `{ code, message }` 포맷으로 통일:

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 유효성 위반(`amount ≤ 0`, `course_id`/`student_id` 공백) | `400` | `{ "code": 400, "message": "amount: ..." }` |
| `paid_at` 형식 오류(`yyyy-MM-dd HH:mm:ss`가 아님) | `400` | `{ "code": 400, "message": "요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요." }` |
| 존재하지 않는 강의 | `404` | `{ "code": 404, "message": "존재하지 않는 강의입니다: course-없음" }` |

### 판매 내역 목록 조회 — `GET /api/sale-record/list`
판매 목록을 조회한다. `creator_id`·기간(`from`/`to`, `YYYY-MM-DD`, `paid_at` 기준/KST)은 **모두 선택**이며 각각 독립 적용된다(생략 시 해당 조건 없이 전체). `paid_at` 오름차순 정렬. 응답에는 연관된 강의명·크리에이터 정보를 함께 담는다.

요청
```http
GET /api/sale-record/list?creator_id=creator-1&from=2025-03-01&to=2025-03-31
```

응답 `200 OK`
```json
[
  { "id": "sale-1", "course_id": "course-1", "course_title": "Spring Boot 입문", "creator_id": "creator-1", "creator_name": "김강사", "student_id": "student-1", "amount": 50000, "paid_at": "2025-03-05 10:00:00" },
  { "id": "sale-2", "course_id": "course-1", "course_title": "Spring Boot 입문", "creator_id": "creator-1", "creator_name": "김강사", "student_id": "student-2", "amount": 50000, "paid_at": "2025-03-15 14:30:00" }
]
```

- `course_title`/`creator_id`/`creator_name`은 참조 ID로 강의·크리에이터를 한 번씩 조회해 in-memory로 채운다(N+1 없음).
- 조건을 모두 생략하면 전체 판매를 반환한다.
- `from > to` → `400`, 날짜 형식 오류 → `400`, `creator_id`가 존재하지 않으면 → `404`.

### 취소(환불) 내역 등록 — `POST /api/cancel-record`
`sale_record_id`는 존재하는 판매여야 하고, `refund_amount`는 0보다 커야 한다. **누적 환불액(기존 + 이번)이 원결제 금액을 넘을 수 없다**(부분 환불 지원). 서버가 `id`를 생성한다.
`canceled_at`은 `paid_at`과 동일하게 **`yyyy-MM-dd HH:mm:ss`(KST) 형식만 허용**한다(다른 형식은 `400`).

요청
```http
POST /api/cancel-record
Content-Type: application/json

{
  "sale_record_id": "sale-4",
  "refund_amount": 30000,
  "canceled_at": "2025-07-01 10:00:00"
}
```

응답 `200 OK`
```json
{
  "id": "cancel-9a1b…",
  "sale_record_id": "sale-4",
  "refund_amount": 30000,
  "canceled_at": "2025-07-01 10:00:00"
}
```

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 유효성 위반(`refund_amount ≤ 0`, `sale_record_id` 공백) | `400` | `{ "code": 400, "message": "refund_amount: ..." }` |
| 환불 요청 금액이 환불 가능 잔액(결제액 − 기환불액) 초과 | `400` | `{ "code": 400, "message": "환불 요청 금액이 환불 가능 잔액을 초과합니다: 잔액 50000, 요청 60000" }` |
| 존재하지 않는 판매 | `404` | `{ "code": 404, "message": "존재하지 않는 판매 내역입니다: sale-없음" }` |

### 크리에이터 월별 정산 계산 — `GET /api/settlement/monthly`
쿼리 파라미터 `creator_id`, `month`(`YYYY-MM`)로 해당 월의 정산을 계산한다. 판매는 `paid_at`, 취소는 `canceled_at` 기준 / KST, 월 경계는 1일 00:00:00 ~ 말일 23:59:59.

요청
```http
GET /api/settlement/monthly?creator_id=creator-1&month=2025-03
```

응답 `200 OK`
```json
{
  "creator_id": "creator-1",
  "month": "2025-03",
  "total_sales": 260000,
  "total_refund": 110000,
  "net_sales": 150000,
  "fee_rate": 0.20,
  "fee": 30000,
  "settlement_amount": 120000,
  "sale_count": 4,
  "cancel_count": 2
}
```

- `net_sales = total_sales − total_refund`, `fee = net_sales × fee_rate`(반올림), `settlement_amount = net_sales − fee`
- `fee_rate`는 **해당 월에 유효한 수수료율**(수수료율 변경 이력 기준)이다. 예: 시드 기준 2025-06까지 20%, 2025-07부터 15%.
- **존재하는** 크리에이터인데 해당 월 판매가 없으면 모두 `0`으로 응답한다(빈 월).
- 특정 월에 취소만 있거나 취소액이 판매액보다 크면 `net_sales`·`settlement_amount`가 **음수**가 될 수 있다(공식을 일관 적용).

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 잘못된 연월 형식 | `400` | `{ "code": 400, "message": "잘못된 연월 형식입니다: ..." }` |
| 존재하지 않는 크리에이터 | `404` | `{ "code": 404, "message": "존재하지 않는 크리에이터입니다: creator-없음" }` |

### 운영자용 기간 정산 집계 — `GET /api/settlement/summary`
쿼리 파라미터 `from`, `to`(`YYYY-MM-DD`, 경계 포함/KST) 기간의 **크리에이터별 정산 목록 + 전체 정산 합계**. 해당 기간에 판매·환불이 하나라도 있는 크리에이터만 포함한다.

> **기간은 자유롭게 지정할 수 있다.** 월 단위(`2025-03-01 ~ 2025-03-31`)뿐 아니라 월을 가로지르거나 부분 월인 임의 구간(예: `2025-02-15 ~ 2025-03-31`)도 그대로 집계된다. 딱 떨어지는 한 달 정산은 `GET /api/settlement/monthly`, 임의 구간 집계는 이 엔드포인트로 역할을 나눴다.

요청
```http
GET /api/settlement/summary?from=2025-03-01&to=2025-03-31
```

응답 `200 OK`
```json
{
  "from": "2025-03-01",
  "to": "2025-03-31",
  "fee_rate": 0.20,
  "total_settlement_amount": 392000,
  "creators": [
    { "creator_id": "creator-1", "total_sales": 260000, "total_refund": 110000, "net_sales": 150000, "fee": 30000, "settlement_amount": 120000, "sale_count": 4, "cancel_count": 2 },
    { "creator_id": "creator-2", "total_sales": 60000,  "total_refund": 0,      "net_sales": 60000,  "fee": 12000, "settlement_amount": 48000,  "sale_count": 1, "cancel_count": 0 },
    { "creator_id": "creator-4", "total_sales": 440000, "total_refund": 160000, "net_sales": 280000, "fee": 56000, "settlement_amount": 224000, "sale_count": 5, "cancel_count": 4 }
  ]
}
```

- `total_settlement_amount`는 목록의 `settlement_amount` 합계.
- 수수료는 **구간을 월별로 분해해 각 월에 유효한 수수료율로 계산 후 합산**한다. 응답의 `fee_rate`는 구간 내 적용 율이 하나일 때만 값이 있고, 율 변경을 가로지르면 `null`이다(크리에이터별 `fee`는 월별 율로 정확히 계산됨).
- `from > to` → `400`, 날짜 형식 오류 → `400`.

### 정산 생성(확정 스냅샷) — `POST /api/settlement`
`creator_id` + `month`(`YYYY-MM`) 단위로 정산을 **스냅샷으로 저장**한다. 금액은 생성 시점의 월별 정산 계산으로 고정되며(이후 판매/취소 추가 미반영), 생성 당시 수수료율(`fee_rate`)도 함께 저장한다. 초기 상태는 `PENDING`.
**동일 크리에이터+월에 정산이 이미 존재하면 상태와 무관하게 `409`로 거부**한다(서비스 사전 조회 + DB 유니크 제약 `(creator_id, period)` 이중 방어).

요청
```http
POST /api/settlement
Content-Type: application/json

{
  "creator_id": "creator-1",
  "month": "2025-03"
}
```

응답 `200 OK`
```json
{
  "id": "settlement-8328e301-…",
  "creator_id": "creator-1",
  "month": "2025-03",
  "status": "PENDING",
  "total_sales": 260000,
  "total_refund": 110000,
  "net_sales": 150000,
  "fee_rate": 0.20,
  "fee": 30000,
  "settlement_amount": 120000,
  "sale_count": 4,
  "cancel_count": 2,
  "created_at": "2025-07-01 10:00:00",
  "confirmed_at": null,
  "paid_at": null
}
```

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 유효성 위반(`creator_id`/`month` 공백) | `400` | `{ "code": 400, "message": "creator_id: ..." }` |
| 잘못된 연월 형식 | `400` | `{ "code": 400, "message": "잘못된 연월 형식입니다: 2025-13 (예: 2025-03)" }` |
| 존재하지 않는 크리에이터 | `404` | `{ "code": 404, "message": "존재하지 않는 크리에이터입니다: creator-없음" }` |
| 동일 크리에이터+월 정산 존재(상태 무관) | `409` | `{ "code": 409, "message": "이미 해당 월의 정산이 존재합니다: creator-1 / 2025-03 (상태: PENDING)" }` |

### 정산 확정 — `POST /api/settlement/{id}/confirm`
`PENDING` 상태의 정산을 `CONFIRMED`로 전이하고 `confirmed_at`을 기록한다. 상태 전이는 **PENDING → CONFIRMED → PAID 단방향만 허용**하며, 그 외(역행·재호출)는 `409`.

요청
```http
POST /api/settlement/settlement-8328e301-…/confirm
```

응답 `200 OK` — 생성 응답과 동일 형식(`status: "CONFIRMED"`, `confirmed_at` 채워짐)

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 존재하지 않는 정산 | `404` | `{ "code": 404, "message": "존재하지 않는 정산입니다: settlement-없음" }` |
| 현재 상태가 `PENDING`이 아님 | `409` | `{ "code": 409, "message": "PENDING 상태의 정산만 확정할 수 있습니다: 현재 PAID" }` |

### 정산 지급 — `POST /api/settlement/{id}/pay`
`CONFIRMED` 상태의 정산을 `PAID`로 전이하고 `paid_at`을 기록한다. `PENDING`에서 바로 지급(건너뛰기)은 `409`.

요청
```http
POST /api/settlement/settlement-8328e301-…/pay
```

응답 `200 OK` — 생성 응답과 동일 형식(`status: "PAID"`, `paid_at` 채워짐)

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 존재하지 않는 정산 | `404` | `{ "code": 404, "message": "존재하지 않는 정산입니다: settlement-없음" }` |
| 현재 상태가 `CONFIRMED`가 아님 | `409` | `{ "code": 409, "message": "CONFIRMED 상태의 정산만 지급 처리할 수 있습니다: 현재 PENDING" }` |

### 정산 목록 조회 — `GET /api/settlement/list`
저장된 정산 스냅샷 목록을 조회한다. `creator_id`·`month`(`YYYY-MM`)·`status`(`PENDING`/`CONFIRMED`/`PAID`)는 **모두 선택**이며 각각 독립 적용된다(생략 시 전체). `created_at` 내림차순 정렬.

요청
```http
GET /api/settlement/list?creator_id=creator-1&status=PAID
```

응답 `200 OK` — 생성 응답과 동일 형식의 배열

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 잘못된 연월 형식(`month`) | `400` | `{ "code": 400, "message": "잘못된 연월 형식입니다: ..." }` |
| 잘못된 상태 값(`status`) | `400` | `{ "code": 400, "message": "요청 파라미터 형식이 올바르지 않습니다: status" }` |

### 정산 목록 CSV 다운로드 — `POST /api/settlement/download/csv`
- 정산 목록을 CSV 파일로 다운로드한다.
- **필터는 목록 조회(`/list`)와 동일한 항목을 JSON 본문**으로 받으며(`creator_id`·`month`·`status` 모두 선택), **본문을 생략하면 전체**를 내려받는다.
- 결과가 없으면 헤더 행만 담긴 파일을 반환한다. 
- 파일은 **UTF-8 BOM**을 포함해 엑셀에서 한글이 깨지지 않고 바로 열린다. 컬럼 헤더는 한글이다(운영자가 파일을 바로 읽는 용도).

요청
```http
POST /api/settlement/download/csv
Content-Type: application/json

{
  "status": "PAID"
}
```

응답 `200 OK` (`Content-Type: text/csv;charset=UTF-8`, `Content-Disposition: attachment; filename="settlements.csv"`)
```csv
정산 ID,크리에이터 ID,정산 월,상태,총 판매,총 환불,순 판매,수수료율,수수료,정산 예정 금액,판매 건수,취소 건수,생성 일시,확정 일시,지급 일시
settlement-3,creator-1,2025-04,PAID,130000,80000,50000,0.2000,10000,40000,2,1,2025-05-01 10:00:00,2025-05-03 10:00:00,2025-05-10 10:00:00
settlement-2,creator-4,2025-03,PAID,440000,160000,280000,0.2000,56000,224000,5,4,2025-04-01 10:00:00,2025-04-03 10:00:00,2025-04-10 10:00:00
```

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 잘못된 연월 형식(`month`) | `400` | `{ "code": 400, "message": "잘못된 연월 형식입니다: ..." }` |
| 잘못된 상태 값(`status`) 등 본문 파싱 실패 | `400` | `{ "code": 400, "message": "요청 본문을 읽을 수 없습니다. 형식을 확인해 주세요." }` |
| POST가 아닌 메서드(GET 등) | `405` | `{ "code": 405, "message": "지원하지 않는 HTTP 메서드입니다: GET" }` |

### 정산 목록 엑셀 다운로드 — `POST /api/settlement/download/excel`
정산 목록을 엑셀(xlsx) 파일로 다운로드한다. 필터(JSON 본문, 생략 가능)·컬럼 구성은 CSV 다운로드와 동일하며, 금액·건수는 숫자 셀로 저장된다.

요청
```http
POST /api/settlement/download/excel
Content-Type: application/json

{
  "creator_id": "creator-4"
}
```

응답 `200 OK` (`Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="settlements.xlsx"`)

### 정산 단건 조회 — `GET /api/settlement/{id}`
정산 스냅샷 단건을 조회한다.

요청
```http
GET /api/settlement/settlement-8328e301-…
```

응답 `200 OK` — 생성 응답과 동일 형식

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 존재하지 않는 정산 | `404` | `{ "code": 404, "message": "존재하지 않는 정산입니다: settlement-없음" }` |

### 수수료율 변경 등록 — `POST /api/fee-rate`
`effective_month`(`YYYY-MM`)부터 다음 변경 전까지 적용될 수수료율을 등록한다.
**과거 정산의 불변을 위해 소급 등록은 불가** — 적용 시작 월은 다음 달 이후만 허용한다.
율은 **백분율(`fee_rate_percent`, 0 이상 100 미만, 소수점 둘째 자리까지)**로 받아 소수(scale 4)로 변환해 저장한다. 예: `12.5` → `0.1250`.

> 성격상 운영자(admin) 전용 API여야 하지만, 이 과제에서는 인증/인가가 범위 외이므로 **일반 API로 제공**한다.

요청
```http
POST /api/fee-rate
Content-Type: application/json

{
  "effective_month": "2026-09",
  "fee_rate_percent": 12.5
}
```

응답 `200 OK`
```json
{
  "id": "fee-rate-c8195ad4-…",
  "effective_month": "2026-09",
  "fee_rate": 0.1250,
  "fee_rate_percent": 12.50,
  "created_at": "2026-07-02 17:23:39"
}
```

| 상황 | 상태 | 본문 예시 |
|------|------|-----------|
| 유효성 위반(`fee_rate_percent` 0 미만·100 이상, 소수 3자리 이상) | `400` | `{ "code": 400, "message": "feeRatePercent: ..." }` |
| 잘못된 연월 형식 | `400` | `{ "code": 400, "message": "잘못된 연월 형식입니다: 2026-13 (예: 2025-03)" }` |
| 현재 월 이하(소급) | `400` | `{ "code": 400, "message": "수수료율은 다음 달 이후부터 적용할 수 있습니다(소급 변경 불가): 2025-01" }` |
| 동일 적용 월 중복 | `409` | `{ "code": 409, "message": "이미 해당 월부터 적용되는 수수료율이 존재합니다: 2026-09 (율: 0.1250)" }` |

### 수수료율 이력 조회 — `GET /api/fee-rate/list`
수수료율 변경 이력을 적용 시작 월 내림차순으로 반환한다.

요청
```http
GET /api/fee-rate/list
```

응답 `200 OK`
```json
[
  { "id": "fee-rate-2", "effective_month": "2025-07", "fee_rate": 0.1500, "fee_rate_percent": 15.00, "created_at": "2025-06-15 10:00:00" },
  { "id": "fee-rate-1", "effective_month": "2024-01", "fee_rate": 0.2000, "fee_rate_percent": 20.00, "created_at": "2023-12-15 10:00:00" }
]
```

## 데이터 모델 설명
연관관계 매핑 없이 각 엔티티는 참조 대상을 **ID 값**으로만 보유한다. (스키마는 Flyway `V1__init.sql`이 소유)

| 엔티티 / 테이블 | 컬럼 | 설명 |
|-----------------|------|------|
| `Creator` / `creator` | `id`, `name` | 크리에이터 |
| `Course` / `course` | `id`, `creatorId`, `title` | 강의 (→ creator 참조) |
| `SaleRecord` / `sale_record` | `id`, `courseId`, `studentId`, `amount`, `paidAt` | 판매 내역 (→ course 참조) |
| `CancelRecord` / `cancel_record` | `id`, `saleRecordId`, `refundAmount`, `canceledAt` | 취소(환불) 내역 (→ sale_record 참조) |
| `Settlement` / `settlement` | `id`, `creatorId`, `period`, `status`, 금액 스냅샷(`totalSales`…`settlementAmount`), `feeRate`, `createdAt`, `confirmedAt`, `paidAt` | 정산 확정 스냅샷 (→ creator 참조). `(creator_id, period)` 유니크 제약으로 중복 정산 방지, `status`는 enum을 `VARCHAR`(STRING)로 저장 |
| `FeeRateHistory` / `fee_rate_history` | `id`, `effectiveMonth`, `feeRate`, `createdAt` | 수수료율 변경 이력. `effectiveMonth`("YYYY-MM")부터 다음 변경 전까지 적용, `effective_month` 유니크 |

- ID는 비즈니스 키(`creator-1`, `sale-1` …)를 그대로 쓰는 문자열 자연키.
- 금액(`amount`, `refundAmount`)은 원 단위 정수(`Long`/`BIGINT`), 시각(`paidAt`, `canceledAt`)은 `LocalDateTime`(KST).
- 정산 대상 연월 컬럼은 `period`(`"YYYY-MM"`)다 — H2에서 `MONTH`가 예약어라 컬럼명으로 쓰지 않는다(API에서는 `month`로 노출).

### 샘플 데이터 & 검증 시나리오
- 애플리케이션 시작 시 Flyway 시드 마이그레이션(`V2__seed_sample_data.sql` 판매/취소, `V4__seed_settlement_data.sql` 정산 스냅샷, `V6__seed_fee_rate_history.sql` 수수료율 이력)으로 자동 적재된다.
- 수수료율 이력 시드: **2024-01부터 20%, 2025-07부터 15%(데모용 인하)**. 기존 검증 시나리오 구간(2024-11 ~ 2025-06)은 전부 20%라 시나리오 수치에 영향이 없다.
- 명세 제공 시나리오(`명세`)는 그대로 보존하고, 직접 검증용 케이스(`추가`)는 명세 시나리오와 겹치지 않는 크리에이터/월만 사용한다.
- 정산 스냅샷 시드(`settlement-1`~`5`)는 V2 판매/취소 데이터로 계산한 값을 그대로 저장했으며 상태별(PENDING 2·CONFIRMED 1·PAID 2)로 구성했다 — 부팅 직후 목록/전이 API를 바로 시연할 수 있다. 시드 수치가 실제 계산과 일치하는지는 `SettlementServiceTest` › `시드 정산 - 스냅샷 금액이 판매·취소 데이터 계산과 일치한다`로 검증한다.

각 시나리오가 어느 테스트에서 검증되는지 마지막 열에 표기한다(리뷰어가 시나리오 → 테스트로 추적 가능). 메서드명은 해당 파일에서 검색 가능한 고유 문구다.

| 구분 | 케이스 | 데이터 | 기대 결과 / 검증 포인트 | 검증 테스트 |
|------|--------|--------|------------------------|-------------|
| 명세 | creator-1 2025-03 정산 | sale-1~4 / cancel-1,2 | 총 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000 | `SettlementServiceTest` › `명세 시나리오 - creator-1 2025-03 정산` |
| 명세 | 부분 환불 | sale-4(80,000) / cancel-2(30,000) | 환불액 < 원결제액이 순 판매에 반영(위 시나리오의 환불 110,000에 포함) | `SettlementServiceTest` › `명세 시나리오 - creator-1 2025-03 정산` |
| 명세 | 월 경계 | sale-5(1/31 판매) / cancel-3(2/2 취소) | 판매는 1월, 취소는 2월 정산에 각각 반영 | `SettlementServiceTest` › `월 경계 - 1월 판매…`, `월 경계 + 음수 - 2월…` |
| 명세 | 빈 월 | creator-3 2025-03 | 판매 0건 → 0원 응답 | `SettlementServiceTest` › `빈 월 - creator-3 2025-03 은 모두 0` |
| 추가 | 한 판매에 다수 부분취소 | sale-105(120,000) / cancel-103(30,000)+cancel-104(20,000) | 한 판매에 취소 2건, 누적 환불 50,000 → 순 기여 70,000 (creator-4 3월 합계 순 280,000에 포함) | `SettlementServiceTest` › `추가 시나리오 - creator-4 2025-03` |
| 추가 | 판매와 같은 날 취소 | sale-103(3/18 판매 90,000) / cancel-102(3/18 환불 40,000) | 판매일=취소일이어도 각각 3월에 반영 | `SettlementServiceTest` › `추가 시나리오 - creator-4 2025-03` |
| 추가 | 월 시작·끝 경계 시각 | sale-106(`04-01 00:00:00`), sale-107(`04-30 23:59:59`) | 두 판매 모두 **4월에 포함** → creator-4 2025-04 판매 2건·160,000 | `SettlementServiceTest` › `추가 시나리오 - creator-4 2025-04` |
| 추가 | 크로스먼스 전액환불 | sale-106(4월 판매 70,000) / cancel-105(5월 취소 70,000) | creator-4 2025-05는 판매 0·환불 70,000 → **순매출 −70,000, 정산 −56,000** | `SettlementServiceTest` › `추가 시나리오 - creator-4 2025-05` |
| 추가 | 동월 전액환불 | sale-109(5월 150,000) / cancel-106(5월 150,000) | 같은 달 전액환불이 환불에 반영(sale-108은 유지) → creator-5 2025-05 순 150,000 | `SettlementServiceTest` › `추가 시나리오 - creator-5 2025-05` |
| 추가 | 재구매(같은 강의) | student-15: sale-108(5월), sale-110(6월) — 둘 다 course-9 | 같은 수강생 재구매도 **각 월에 독립 집계** → creator-5 2025-06 판매 1건 | `SettlementServiceTest` › `추가 시나리오 - creator-5 2025-06` |
| 추가 | 전년도(2024) 데이터 | sale-115(2024-12), sale-121(2024-11) | 2025-03 조회 시 2024년 판매 **제외**(creator-1 2025-03 목록 4건) | `SaleServiceTest` › `목록 조회 - 크리에이터 + 기간 필터` |
| 추가 | 다건 판매 볼륨 월 | creator-4 2025-06: sale-122~125 / cancel-110,111 | 판매 4건 320,000 · 환불 135,000 → 순 185,000 · 수수료 37,000 · 정산 148,000 | `SettlementServiceTest` › `다건 볼륨 - creator-4 2025-06` |

## 요구사항 해석 및 가정
- 모든 시각은 **KST 기준**으로 저장·해석한다. DB 컬럼은 타임존 없는 `TIMESTAMP`(엔티티 `LocalDateTime`)를 사용한다. **타임존은 별도로 고려하지 않으며**, API의 시각 입출력은 **`yyyy-MM-dd HH:mm:ss` 단일 형식**으로 통일하고 그 값을 KST 벽시계로 해석한다. 타임존/오프셋이 붙은 ISO-8601 형식(`2025-03-05T10:00:00+09:00` 등)은 지원하지 않는다(`400`).
- **인증/인가는 범위 외**로 둔다. 과제가 "userId를 헤더/파라미터로 전달" 방식을 허용하므로 크리에이터 식별은 API 파라미터(`creator_id`)로 처리하고, 운영자 집계 API도 역할 검증 없이 노출한다.
- **월별 정산과 기간 집계의 기간 정의를 분리**했다. 월별(`/monthly`)은 `YYYY-MM`으로 해당 월 1일~말일, 기간 집계(`/summary`)는 `from`~`to`의 임의 구간.
- **수수료**는 순 판매액 × 수수료율(기본 20%)이며 원 단위로 **반올림**(HALF_UP)한다.
- 특정 기간에 환불이 판매를 초과하면 순 판매·정산액이 **음수**가 될 수 있으며, 공식(`정산 = 순 판매 − 수수료`)을 음수에도 일관 적용한다.
- 월별 정산 조회 시 **존재하지 않는 크리에이터는 `404`**, **존재하지만 해당 월 판매가 없으면 `0`(빈 월)**으로 구분해 응답한다.
- **정산 확정은 크리에이터+월 단위**로만 지원한다(월별 정산 계산과 동일 단위). "동일 기간 중복 정산"의 기간 정의도 이 단위다.
- **중복 정산은 상태와 무관하게 무조건 `409`로 거부**한다. 기존 정산 삭제·재계산(재정산)은 범위 외.
- **상태 전이는 PENDING → CONFIRMED → PAID 단방향만** 허용한다. 역행·건너뛰기·재호출은 모두 `409`(리소스 현재 상태와의 충돌).
- **정산 스냅샷은 생성 시점 계산으로 고정**된다. 생성 후 해당 월에 판매/취소가 추가되어도 스냅샷에 반영되지 않는다(즉석 계산 API인 `/monthly`는 반영됨).
- 스냅샷에는 **생성 당시 수수료율(`fee_rate`)을 함께 저장**한다 — 이후 율이 바뀌어도 확정된 정산은 불변.
- **수수료율 변경은 월 단위로만 적용**된다("YYYY-MM부터"). 정산이 월 단위인 도메인과 일치시키고, 월 중간 변경(건별 율 매칭) 복잡도를 배제했다.
- **수수료율 소급 등록 불가**: 적용 시작 월은 다음 달 이후만 허용. 과거 월의 율이 바뀌면 "과거 정산은 당시 율" 원칙(스냅샷·즉석 계산 일치)이 깨지기 때문.
- 기간 집계(summary)는 **구간을 월별로 분해해 각 월의 율로 수수료를 계산·합산**한다. 같은 율이 적용된 순매출끼리 묶어 계산하므로, 구간 내 율이 하나면 분해 전과 결과가 동일하다(반올림 차이 없음).

## 설계 결정과 이유

### JPA 연관관계 매핑 미사용 (FK ID만 보유)
- `@OneToMany`, `@ManyToOne` 등 JPA 연관관계 매핑을 사용하지 않고, 엔티티 간 관계는 **FK ID 값만 보유**합니다.
- N+1 문제, 묵시적 JOIN, Lazy/Eager 로딩 이슈 등을 구조적으로 차단하고 실행 쿼리를 예측 가능하게 유지하기 위함입니다.
- 연관 데이터가 필요한 경우 **Repository를 통해 명시적으로 조회**합니다.

### 조회는 QueryDSL 커스텀 리포지토리로만
- `JpaRepository`는 쓰기(`save` 등) 위주로 쓰고, **모든 조회는 커스텀 프래그먼트**(`XxxCustomRepository` + `XxxCustomRepositoryImpl`, `JPAQueryFactory` 주입)에서 QueryDSL로 작성합니다. 조회 파라미터는 `repository/dto`에 정의합니다.
- 연관관계가 없으므로 집계 시 조인을 `join(...).on(a.xxxId.eq(b.id))`로 명시합니다(예: 판매→강의, 취소→판매→강의). 타입 안전하고 실행 쿼리가 예측 가능합니다.

### 연관 데이터는 in-memory로 조립 (N+1 방지)
응답에 연관 엔티티 정보(강의명, 크리에이터명 등)가 필요할 때, JPA 연관관계 대신 **참조 ID를 모아 한 번에 조회**한 뒤 `@Transient` 필드에 in-memory로 매핑합니다: `List<SaleRecord>.fromCourses(...)` → 그 안에서 `List<Course>.fromCreators(...)`. 목록 크기와 무관하게 **course 1회·creator 1회** 조회로 끝나 N+1이 발생하지 않고, 결합 시점과 대상을 코드에서 명시적으로 통제합니다. (판매 목록 조회 응답의 `course_title`/`creator_id`/`creator_name`이 이 방식으로 채워집니다.)

### 수수료율을 정책 객체로 분리 + 월 단위 변경 이력
수수료율 결정과 수수료 계산은 `FeePolicy` 컴포넌트가 전담합니다. 율은 `fee_rate_history` 테이블의 **월 단위 변경 이력**으로 관리되며, `rateFor(연월)`이 "적용 시작 월 ≤ 대상 월" 중 가장 최신 이력을 찾아 반환합니다(이력이 없는 과거 구간은 설정 `settlement.fee-rate` 기본값). 과거 정산의 불변은 두 겹으로 보장됩니다: ① 확정 스냅샷은 생성 당시 율을 저장하고, ② 율의 소급 등록 자체를 막습니다(다음 달 이후만 허용). 율 비교·그룹핑 시 `BigDecimal` scale 차이로 같은 율이 다르게 취급되지 않도록 `rateFor`는 scale 4로 정규화해 반환합니다.

### 정산 상태 관리: 스냅샷 영속화 + 이중 방어
- 정산 확정은 조회 시점 즉석 계산과 분리해 **`settlement` 테이블에 스냅샷으로 영속화**합니다. 월별 정산 조회(`getMonthly`)와 스냅샷 생성(`create`)은 같은 private 계산 메서드(`calculateMonthly`)를 공유해 두 경로의 수치가 어긋나지 않습니다(공개 서비스 메서드 간 self-invocation은 `@Transactional` 프록시를 우회하므로 지양).
- 중복 정산 방지는 **서비스 사전 조회(1차) + DB 유니크 제약 `(creator_id, period)`(2차)** 이중 방어입니다. 사전 조회를 통과한 경합 케이스는 제약 위반(`DataIntegrityViolationException`)이 전역 핸들러에서 `409`로 응답됩니다.
- 잘못된 상태 전이도 `400`이 아닌 **`409`** 를 씁니다 — RFC 9110의 409는 "리소스의 현재 상태와의 충돌"이고, 이 프로젝트에서 `400`은 요청 형식 오류에 일관되게 쓰고 있기 때문입니다.
- 상태 전이 검증은 서비스가 아닌 **엔티티 메서드(`Settlement.confirm`/`pay`)** 에 두어, 상태 불변식이 상태와 같은 곳에서 강제되도록 했습니다.

### CSV/엑셀 내보내기는 컨트롤러(표현 계층)에서 변환
- 다운로드 API는 목록 조회와 **동일한 서비스 메서드(`SettlementService.list`)를 재사용**하고, 그 결과를 CSV/xlsx로 직렬화하는 작업만 컨트롤러에서 수행합니다.
- "같은 데이터를 어떤 표현으로 내보내느냐"는 표현 계층의 관심사라는 판단이며, 덕분에 필터·정렬 동작이 목록 API와 어긋날 수 없습니다.
- CSV와 엑셀은 공용 컬럼 정의(헤더명 + 값 추출)를 공유해 두 포맷의 컬럼이 항상 일치합니다.

### 예외 처리 일원화 (ErrorCode + 전역 핸들러)
- 도메인 예외는 단일 `CreatorException(errorCode, message = errorCode.message)`로 던지고, `ErrorCode` enum이 `(HTTP 상태 코드, 기본 메시지)`를 보유합니다. 기본 메시지를 쓰거나 던질 때 상황별 메시지를 주입할 수 있습니다.
- `@RestControllerAdvice`(전역 핸들러) 한 곳에서 모든 예외를 **`{ code, message }` 단일 포맷**으로 변환합니다.

| 예외 | 상태 | 케이스 |
|------|------|--------|
| `CreatorException` | `errorCode.code` | 도메인 규칙 위반(강의/크리에이터 없음=404, 환불 초과 등=400, 중복 정산·잘못된 상태 전이=409) |
| `DataIntegrityViolationException` | `409` | DB 무결성 제약 위반(유니크 제약 등 — 사전 검증을 통과한 경합의 2차 방어) |
| `MethodArgumentNotValidException` | `400` | `@Valid` 본문 검증 실패 |
| `MethodArgumentTypeMismatchException` | `400` | 쿼리 파라미터 타입 오류(예: 날짜 형식) |
| `MissingServletRequestParameterException` | `400` | 필수 쿼리 파라미터 누락 |
| `HttpMessageNotReadableException` | `400` | 잘못된 JSON·필수 필드 누락 등 본문 파싱 실패 |
| `HttpRequestMethodNotSupportedException` | `405` | 지원하지 않는 HTTP 메서드(예: POST 전용 다운로드에 GET) |
| `NoResourceFoundException` | `404` | 매핑되지 않은 경로 |
| 그 외 `Exception` | `500` | 예상치 못한 오류(내부 메시지 미노출, 서버 로그로 기록) |

## 테스트 실행 방법
```bash
./gradlew test                                        # 전체 테스트
./gradlew test --tests "*SaleServiceTest"             # 특정 테스트 클래스
```
- 테스트는 `@SpringBootTest` + 인메모리 H2로 실제 스택(Flyway 시드 포함)에서 동작하며, `@Transactional`로 각 테스트 후 롤백되어 데이터가 오염되지 않는다.
- 위 「샘플 데이터 & 검증 시나리오」의 각 케이스는 표의 `검증 테스트` 열에 표기된 테스트로 검증한다.
- 그 외에 등록/조회 성공·실패, 검증 실패, 예외 처리 등 **동작 검증용 service 테스트**(`SaleServiceTest` / `CancelRecordServiceTest` / `SettlementServiceTest`)가 추가로 구현되어 있다.

## 미구현 / 제약사항
- **인증/인가**: 구현하지 않음(범위 외). `creator_id`를 신뢰하고 처리한다. 수수료율 변경·기간 집계 등 운영자 성격 API도 일반 API로 노출된다.
- **DB는 인메모리 H2**: 애플리케이션 종료 시 데이터가 초기화되며, 매 기동 시 Flyway 시드로 동일 데이터가 재적재된다. 운영 DB(MySQL 등) 연동은 범위 외.
- **정산 스냅샷의 재계산·취소 미지원**: 동일 크리에이터+월 정산은 상태 무관 409로 거부되며, 잘못 생성한 정산을 삭제하고 다시 만드는 흐름은 범위 외.

## AI 활용 범위
- 이 프로젝트는 Claude Code(AI 페어 프로그래밍)를 활용해 진행했다.
- 설계 방향과 최종 의사결정은 개발자가 주도하고, 구현·검증·문서화를 AI와 함께했다.

### AI(Claude)가 한 것
- 초기 환경 설정: `.gitignore` 작성, 의존성 설정 및 기본 세팅
- 작업 단위를 **GitHub 이슈로 분해·생성**
- 구현/테스트 코드 작성: Flyway 스키마·시드 데이터, 엔티티·리포지토리, QueryDSL 세팅, API 코드 생성, 전역 예외 핸들러(`@RestControllerAdvice`), 서비스 테스트
- 실제 빌드·테스트·API 호출(E2E)로 **검증**(예: 명세 시나리오 수치 대조)
- `README.md` / `CLAUDE.md` 문서화

### 개발자(본인)가 한 것
- 요구사항 해석과 **핵심 설계 결정**:
  - 연관관계(FK) 없이 ID 참조만으로 설계
  - 시각은 `TIMESTAMP` + `LocalDateTime`(전 구간 KST)
  - 패키지 구조(계층별 + DTO 계층 하위 중첩), 요청/응답 **snake_case 전역**
  - 예외 설계(`ErrorCode` enum + 단일 `CreatorException`)와 전역 핸들러 방식
  - 커스텀 리포지토리 패턴(`fetch`/`fetchOne` + 파라미터 DTO), 조회는 QueryDSL로만
  - 응답용 연관 데이터(course/creator) 조립을 in-memory 배치 방식(`@Transient` + `fromCourses`/`fromCreators` 확장함수)으로 직접 설계·구현
  - 테스트 전략(컨트롤러 테스트는 생략, 서비스/시나리오 테스트 중심)
  - 선택 과제의 도메인 규칙 확정: 정산 확정 단위(크리에이터+월)·단방향 상태 전이·중복 정산 무조건 409, 수수료율의 월 단위 적용·소급 등록 금지, 기간 집계의 월별 분해 합산, 다운로드는 목록 조회를 재사용해 컨트롤러에서 변환
- **AI 산출물 리뷰·교정**: 트랜잭션 처리 방식, 인덱스 누락 등 기술적 결함을 직접 잡아 수정을 지시하고, API 형태(경로·HTTP 메서드·요청/입력 형식)와 사용성 관점의 재설계를 주도했으며, 실익 없는 리팩터링 제안은 반려하는 등 코드 방향과 품질을 통제
- 직접 코드 리팩터링·네이밍 정리(`SaleRecord*` 계열 등), 모든 API 응답·파일 산출물 직접 호출·검수
- 최종 의사결정 및 **모든 커밋 수행**