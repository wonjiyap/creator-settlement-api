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
- 판매가 없는 월은 모두 `0`으로 응답한다(빈 월).
- 특정 월에 취소만 있거나 취소액이 판매액보다 크면 `net_sales`·`settlement_amount`가 **음수**가 될 수 있다(공식을 일관 적용).
- 잘못된 연월 형식 → `400 { "code": 400, "message": "잘못된 연월 형식입니다: ..." }`

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

| 구분 | 케이스 | 데이터 | 기대 결과 / 검증 포인트 |
|------|--------|--------|------------------------|
| 명세 | creator-1 2025-03 정산 | sale-1~4 / cancel-1,2 | 총 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000 |
| 명세 | 부분 환불 | sale-4(80,000) / cancel-2(30,000) | 환불액 < 원결제액 반영 |
| 명세 | 월 경계 | sale-5(1/31 판매) / cancel-3(2/2 취소) | 판매는 1월, 취소는 2월 정산에 각각 반영 |
| 명세 | 빈 월 | creator-3 2025-03 | 판매 0건 → 0원 응답 |
| 추가 | 한 판매에 다수 부분취소 | sale-105(120,000) / cancel-103(30,000)+cancel-104(20,000) | 한 판매에 취소 2건, 누적 환불 50,000 → 순 기여 70,000 (누적 환불이 원금 120,000을 넘지 않음) |
| 추가 | 판매와 같은 날 취소 | sale-103(3/18 판매 90,000) / cancel-102(3/18 환불 40,000) | 판매일=취소일이어도 판매·취소가 각각 3월에 반영, 순 기여 50,000 |
| 추가 | 월 시작·끝 경계 시각 | sale-106(`04-01 00:00:00`), sale-107(`04-30 23:59:59`) | 두 판매 모두 **4월에 포함**(월 경계 00:00:00~23:59:59 양끝 포함 확인) |
| 추가 | 크로스먼스 전액환불 | sale-106(4월 판매 70,000) / cancel-105(5월 취소 70,000) | 판매는 4월, 환불은 5월에 각각 반영 → creator-4 2025-05는 판매 0·환불 70,000 → **순매출 −70,000(음수)** |
| 추가 | 동월 전액환불 | sale-109(5월 150,000) / cancel-106(5월 150,000) | 판매·환불 모두 5월 → **순매출 0** |
| 추가 | 재구매(같은 강의) | student-15: sale-108(5월), sale-110(6월) — 둘 다 course-9 | 같은 수강생이 같은 강의를 5월·6월에 각각 구매 → 판매는 **각 월에 독립 집계**(정산은 수강생 식별과 무관) |
| 추가 | 전년도(2024) 데이터 | sale-115(2024-12), sale-121(2024-11) | 2025년 대상 조회 시 2024년 판매는 **제외** → 연·월 필터가 연도를 구분하는지 확인 |
| 추가 | 다건 판매 볼륨 월 | creator-4 2025-06: sale-122~125 / cancel-110,111 | 판매 4건 320,000 · 환불 135,000(전액 90,000+부분 45,000) → 순 185,000 · 수수료 37,000 · 정산 148,000 |

## 요구사항 해석 및 가정
- 모든 시각은 **KST 기준**으로 저장·해석한다. DB 컬럼은 타임존 없는 `TIMESTAMP`(엔티티 `LocalDateTime`)를 사용하고, 입력에 포함된 `+09:00` 오프셋은 KST 벽시계 값으로 취급한다.

## 설계 결정과 이유

### JPA 연관관계 매핑 미사용 (FK ID만 보유)
- `@OneToMany`, `@ManyToOne` 등 JPA 연관관계 매핑을 사용하지 않고, 엔티티 간 관계는 **FK ID 값만 보유**합니다.
- N+1 문제, 묵시적 JOIN, Lazy/Eager 로딩 이슈 등을 구조적으로 차단하고 실행 쿼리를 예측 가능하게 유지하기 위함입니다.
- 연관 데이터가 필요한 경우 **Repository를 통해 명시적으로 조회**합니다.

### 수수료율을 정책 객체로 분리
수수료율은 현재 고정 20%지만, 값을 코드에 하드코딩하지 않고 설정(`settlement.fee-rate`)으로 분리하고 계산을 `FeePolicy` 컴포넌트에 위임합니다. 수수료율 변경 시 설정만 바꾸면 되고, 추후 "수수료율 변경 이력(과거 정산은 당시 율 적용)"으로 확장할 지점이 명확합니다.

### 예외 처리 일원화 (ErrorCode + 전역 핸들러)
- 도메인 예외는 단일 `CreatorException(errorCode, message = errorCode.message)`로 던지고, `ErrorCode` enum이 `(HTTP 상태 코드, 기본 메시지)`를 보유합니다. 기본 메시지를 쓰거나 던질 때 상황별 메시지를 주입할 수 있습니다.
- `@RestControllerAdvice`(전역 핸들러)가 `CreatorException` → `errorCode.code` 상태 + `{ code, message }` 본문으로, 검증 실패 → `400` 동일 포맷으로 변환합니다. → 응답 포맷이 한 곳에서 통일됩니다.

## 테스트 실행 방법
```bash
./gradlew test                                        # 전체 테스트
./gradlew test --tests "*SaleServiceTest"             # 특정 테스트 클래스
```
- 테스트는 `@SpringBootTest` + 인메모리 H2로 실제 스택(Flyway 시드 포함)에서 동작하며, `@Transactional`로 각 테스트 후 롤백되어 데이터가 오염되지 않는다.

## 미구현 / 제약사항

## AI 활용 범위
- 이 프로젝트는 Claude Code(AI 페어 프로그래밍)를 활용해 진행했다.
- 설계 방향과 최종 의사결정은 개발자가 주도하고, 구현·검증·문서화를 AI와 함께했다.

### AI(Claude)가 한 것
- 초기 환경 설정: `.gitignore` 작성, 의존성 설정 및 기본 세팅
- 작업 단위를 **GitHub 이슈로 분해·생성**
- 구현/테스트 코드 작성: Flyway 스키마·시드 데이터, 엔티티·리포지토리, QueryDSL 세팅, 판매/취소 등록 API, 월별 정산 계산 API, 서비스 테스트
- 실제 빌드·테스트·데이터 집계로 **검증**(예: 명세 시나리오 수치 대조)
- `README.md` / `CLAUDE.md` 문서화

### 개발자(본인)가 한 것
- 요구사항 해석과 **핵심 설계 결정**:
  - 연관관계(FK) 없이 ID 참조만으로 설계
  - 시각은 `TIMESTAMP` + `LocalDateTime`(전 구간 KST)
  - 패키지 구조(계층별 + DTO 계층 하위 중첩), 요청/응답 **snake_case 전역**
  - 예외 설계(`ErrorCode` enum + 단일 `CreatorException`)와 전역 핸들러 방식
  - 커스텀 리포지토리 패턴(`fetch`/`fetchOne` + 파라미터 DTO), 조회는 QueryDSL로만
  - 테스트 전략(컨트롤러 테스트는 생략, 서비스/시나리오 테스트 중심)
- 직접 코드 리팩터링·네이밍 정리(`SaleRecord*` 계열 등), AI 산출물 리뷰 및 수정 지시
- 최종 의사결정 및 **모든 커밋 수행**