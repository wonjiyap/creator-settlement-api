## 프로젝트 개요
크리에이터 정산(Creator Settlement) API입니다.

크리에이터는 강의를 개설해 수강생에게 판매하고, 플랫폼은 판매 금액에서 수수료(20%)를 제한 뒤 크리에이터에게 정산합니다. 수강 취소가 발생하면 이미 계산된 정산 금액에서 환불분이 차감됩니다.

주요 기능은 다음과 같습니다.

- **판매/취소 내역 관리**: 판매 내역 등록, 취소(환불) 내역 등록, 크리에이터별·기간별 판매 내역 조회
- **크리에이터별 월별 정산 계산**: 총 판매 / 환불 / 순 판매 / 수수료 / 정산 예정 금액 및 판매·취소 건수 산출
- **운영자용 정산 집계**: 특정 기간 내 전체 크리에이터의 정산 현황 및 합계 조회

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
- `from > to` → `400`, 날짜 형식 오류 → `400`.

## 데이터 모델 설명
연관관계 매핑 없이 각 엔티티는 참조 대상을 **ID 값**으로만 보유한다. (스키마는 Flyway `V1__init.sql`이 소유)

| 엔티티 / 테이블 | 컬럼 | 설명 |
|-----------------|------|------|
| `Creator` / `creator` | `id`, `name` | 크리에이터 |
| `Course` / `course` | `id`, `creatorId`, `title` | 강의 (→ creator 참조) |
| `SaleRecord` / `sale_record` | `id`, `courseId`, `studentId`, `amount`, `paidAt` | 판매 내역 (→ course 참조) |
| `CancelRecord` / `cancel_record` | `id`, `saleRecordId`, `refundAmount`, `canceledAt` | 취소(환불) 내역 (→ sale_record 참조) |

- ID는 비즈니스 키(`creator-1`, `sale-1` …)를 그대로 쓰는 문자열 자연키.
- 금액(`amount`, `refundAmount`)은 원 단위 정수(`Long`/`BIGINT`), 시각(`paidAt`, `canceledAt`)은 `LocalDateTime`(KST).

### 샘플 데이터 & 검증 시나리오
- 애플리케이션 시작 시 Flyway 시드 마이그레이션(`V2__seed_sample_data.sql`)으로 자동 적재된다.
- 명세 제공 시나리오(`명세`)는 그대로 보존하고, 직접 검증용 케이스(`추가`)는 명세 시나리오와 겹치지 않는 크리에이터/월만 사용한다.

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
- 모든 시각은 **KST 기준**으로 저장·해석한다. DB 컬럼은 타임존 없는 `TIMESTAMP`(엔티티 `LocalDateTime`)를 사용하고, 입력에 포함된 `+09:00` 오프셋은 KST 벽시계 값으로 취급한다.
- **인증/인가는 범위 외**로 둔다. 과제가 "userId를 헤더/파라미터로 전달" 방식을 허용하므로 크리에이터 식별은 API 파라미터(`creator_id`)로 처리하고, 운영자 집계 API도 역할 검증 없이 노출한다.
- **월별 정산과 기간 집계의 기간 정의를 분리**했다. 월별(`/monthly`)은 `YYYY-MM`으로 해당 월 1일~말일, 기간 집계(`/summary`)는 `from`~`to`의 임의 구간.
- **수수료**는 순 판매액 × 수수료율(기본 20%)이며 원 단위로 **반올림**(HALF_UP)한다.
- 특정 기간에 환불이 판매를 초과하면 순 판매·정산액이 **음수**가 될 수 있으며, 공식(`정산 = 순 판매 − 수수료`)을 음수에도 일관 적용한다.
- 월별 정산 조회 시 **존재하지 않는 크리에이터는 `404`**, **존재하지만 해당 월 판매가 없으면 `0`(빈 월)**으로 구분해 응답한다.

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

### 수수료율을 정책 객체로 분리
수수료율은 현재 고정 20%지만, 값을 코드에 하드코딩하지 않고 설정(`settlement.fee-rate`)으로 분리하고 계산을 `FeePolicy` 컴포넌트에 위임합니다. 수수료율 변경 시 설정만 바꾸면 되고, 추후 "수수료율 변경 이력(과거 정산은 당시 율 적용)"으로 확장할 지점이 명확합니다.

### 예외 처리 일원화 (ErrorCode + 전역 핸들러)
- 도메인 예외는 단일 `CreatorException(errorCode, message = errorCode.message)`로 던지고, `ErrorCode` enum이 `(HTTP 상태 코드, 기본 메시지)`를 보유합니다. 기본 메시지를 쓰거나 던질 때 상황별 메시지를 주입할 수 있습니다.
- `@RestControllerAdvice`(전역 핸들러) 한 곳에서 모든 예외를 **`{ code, message }` 단일 포맷**으로 변환합니다.

| 예외 | 상태 | 케이스 |
|------|------|--------|
| `CreatorException` | `errorCode.code` | 도메인 규칙 위반(강의/크리에이터 없음=404, 환불 초과 등=400) |
| `MethodArgumentNotValidException` | `400` | `@Valid` 본문 검증 실패 |
| `MethodArgumentTypeMismatchException` | `400` | 쿼리 파라미터 타입 오류(예: 날짜 형식) |
| `MissingServletRequestParameterException` | `400` | 필수 쿼리 파라미터 누락 |
| `HttpMessageNotReadableException` | `400` | 잘못된 JSON·필수 필드 누락 등 본문 파싱 실패 |
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
- **인증/인가**: 구현하지 않음(범위 외). `creator_id`를 신뢰하고 처리한다.
- **DB는 인메모리 H2**: 애플리케이션 종료 시 데이터가 초기화되며, 매 기동 시 Flyway 시드로 동일 데이터가 재적재된다. 운영 DB(MySQL 등) 연동은 범위 외.

## AI 활용 범위
- 이 프로젝트는 Claude Code(AI 페어 프로그래밍)를 활용해 진행했다.
- 설계 방향과 최종 의사결정은 개발자가 주도하고, 구현·검증·문서화를 AI와 함께했다.

### AI(Claude)가 한 것
- 초기 환경 설정: `.gitignore` 작성, 의존성 설정 및 기본 세팅
- 작업 단위를 **GitHub 이슈로 분해·생성**
- 구현/테스트 코드 작성: Flyway 스키마·시드 데이터, 엔티티·리포지토리, QueryDSL 세팅, 판매 등록/목록 조회·취소 등록 API, 월별 정산 계산·운영자 기간 집계 API, 전역 예외 핸들러(`@RestControllerAdvice`), 서비스 테스트
- 실제 빌드·테스트·데이터 집계로 **검증**(예: 명세 시나리오 수치 대조)
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
- 직접 코드 리팩터링·네이밍 정리(`SaleRecord*` 계열 등), AI 산출물 리뷰 및 수정 지시
- 최종 의사결정 및 **모든 커밋 수행**