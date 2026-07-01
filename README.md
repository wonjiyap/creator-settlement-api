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
| Database | H2 |
| DB Migration | Flyway |
| API Docs | Swagger UI |

## 실행 방법
**요구 사항**: JDK 17 이상 (별도 DB 설치 불필요 — H2 사용)

```bash
# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test
```

실행 후 접속 정보:

| 항목 | URL |
|------|-----|
| API 서버 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/api-docs |
| H2 Console | http://localhost:8080/h2-console |

## API 목록 및 예시
## 데이터 모델 설명
## 요구사항 해석 및 가정
- 모든 시각은 **KST 기준**으로 저장·해석한다. DB 컬럼은 타임존 없는 `TIMESTAMP`(엔티티 `LocalDateTime`)를 사용하고, 입력에 포함된 `+09:00` 오프셋은 KST 벽시계 값으로 취급한다.

## 설계 결정과 이유

### JPA 연관관계 매핑 미사용 (FK ID만 보유)
- `@OneToMany`, `@ManyToOne` 등 JPA 연관관계 매핑을 사용하지 않고, 엔티티 간 관계는 **FK ID 값만 보유**합니다.
- N+1 문제, 묵시적 JOIN, Lazy/Eager 로딩 이슈 등을 구조적으로 차단하고 실행 쿼리를 예측 가능하게 유지하기 위함입니다.
- 연관 데이터가 필요한 경우 **Repository를 통해 명시적으로 조회**합니다.

## 테스트 실행 방법
## 미구현 / 제약사항
## AI 활용 범위