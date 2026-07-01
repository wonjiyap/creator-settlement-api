## 프로젝트 성격

크리에이터 정산(Creator Settlement) API — 과제(assignment)용 Spring Boot 프로젝트입니다.

## 작업 규칙 (중요)

- **커밋은 절대 하지 말 것.** `git commit`(및 `git add`/`push` 등 커밋으로 이어지는 작업)은 사용자가 직접 수행한다. Claude는 코드 수정/파일 생성까지만 하고 커밋은 하지 않는다.
- 각 이슈 작업을 끝낸 뒤에는 `README.md`와 이 `CLAUDE.md`에서 갱신이 필요한 부분(기술 스택, 실행 방법, 도메인 규칙 등)을 한 번씩 점검·수정한다.

## 명령어

```bash
./gradlew bootRun        # 앱 실행 (포트 8080)
./gradlew test           # 전체 테스트
./gradlew test --tests "com.wonjiyap.creatorsettlementapi.*ClassName*"   # 단일 테스트 클래스
./gradlew clean build    # 클린 빌드 (테스트 포함)
./gradlew compileKotlin  # 컴파일만 빠르게 확인
```

실행 후 접속:
- Swagger UI: `http://localhost:8080/api-docs` (커스텀 경로 — `application.yml`의 `springdoc.swagger-ui.path`)
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:settlement`, User `sa`, Password 비움

## 기술 스택 / 버전 제약 (중요)

- Kotlin 2.3.21 (JVM 17), Gradle 9.5.1 (Kotlin DSL), H2 인메모리.
- **Spring Boot는 반드시 3.5.x 라인을 유지할 것.**
- JPA 엔티티를 위해 `kotlin("plugin.jpa")` + `allOpen`이 `@Entity`/`@MappedSuperclass`/`@Embeddable`에 적용되어 있다(엔티티 클래스를 `open`으로 선언할 필요 없음).

### 스키마 / DB
- **스키마는 Flyway가 소유한다.** 마이그레이션 위치: `src/main/resources/db/migration/`(현재 `V1__init.sql`). 테이블 변경은 새 `V{n}__*.sql`을 추가하는 방식으로 한다(기존 파일 수정은 지양 — 단, 인메모리라 매 부팅 이력이 초기화되어 개발 중엔 수정해도 무방).
- Hibernate 자동 DDL은 끈다: `spring.jpa.hibernate.ddl-auto: validate` → 엔티티 매핑이 Flyway 스키마와 어긋나면 부팅 시 검출된다.
- ID는 샘플 데이터의 비즈니스 키(`creator-1`, `sale-1` 등)를 그대로 쓰는 **VARCHAR 자연키**. 금액은 원 단위 `BIGINT`.
- **모든 시각은 KST 기준**: 컬럼은 `TIMESTAMP`(WITHOUT TIME ZONE, MySQL `DATETIME` 급), 엔티티는 `LocalDateTime`으로 매핑. 입력의 `+09:00` 오프셋은 벽시계 값으로 취급.
- **연관관계(FK 제약)를 두지 않는다.** 테이블 간 참조는 ID 값으로만 느슨하게 연결하며, JPA 엔티티도 `@ManyToOne`/`@OneToMany` 없이 참조 ID를 일반 컬럼(예: `courseId: String`)으로 갖는다. (조인이 필요한 조회는 리포지토리/쿼리에서 명시적으로 처리)
- Flyway H2 지원은 `flyway-core`에 포함되어 별도 `flyway-database-h2` 모듈 불필요.

## 도메인 / 비즈니스 규칙 (구현 기준)

정산 계산 로직의 정확성이 이 과제의 핵심이다. 아래 규칙을 코드/테스트에서 반드시 지킬 것.

- **수수료율: 고정 20%.** 단, 변경 가능하도록 설계할 것(상수 하드코딩 지양, 추후 수수료율 이력 관리로 확장 여지). 정산 예정 금액 = 순 판매 − 수수료.
- **순 판매 = 총 판매 − 환불.** 부분 환불(환불액 < 원결제액)을 반드시 지원.
- **정산 기간 기준**: 판매는 결제 완료 일시(`paidAt`), 취소는 취소 일시 기준. **KST**. 월 경계는 해당 월 1일 `00:00:00` ~ 말일 `23:59:59`. → 1월 말 결제 / 2월 초 취소는 각각 다른 월 정산에 반영된다(월 경계 케이스).
- 빈 월 조회(판매 없음)는 0원으로 일관되게 응답.

### API 범위
- 필수: ① 판매/취소 내역 등록 및 크리에이터별·기간 필터 조회, ② 크리에이터별 월별 정산 계산(요청: 크리에이터 ID + 연월 `YYYY-MM`; 응답: 총 판매/환불/순 판매/수수료/정산 예정 금액 + 판매·취소 건수), ③ 운영자용 기간 집계(시작일~종료일 → 크리에이터별 정산 예정 금액 목록 + 전체 합계).
- 선택(가산점): 정산 상태 관리(PENDING→CONFIRMED→PAID), 동일 기간 중복 정산 방지, CSV/엑셀 다운로드, 수수료율 변경 이력(과거 정산은 당시 수수료율 적용).

실제 결제 시스템 연동은 불필요 — 데이터는 API 또는 직접 삽입으로 등록한다.

## 패키지 / 구조

루트 패키지는 `com.wonjiyap.creatorsettlementapi`. **계층별 패키지 + DTO는 계층 하위에 중첩**한다:

```
domain/            # 엔티티 (Creator, Course, SaleRecord, CancelRecord)
repository/        # Spring Data JPA 리포지토리
  dto/             # 조회 프로젝션 DTO (집계/조인 결과)
service/           # 비즈니스 로직 (필요 시 service 계산 결과 모델)
controller/        # REST 컨트롤러
  dto/             # 요청/응답 DTO
```

- 요청/응답 DTO는 `controller/dto`, 조회 프로젝션은 `repository/dto`. 공용 평면 `dto/` 패키지는 만들지 않는다.

## 문서

상세 과제 명세와 API 예시는 `README.md` 참조(섹션 스켈레톤이 잡혀 있으며 구현하며 채워 나간다).
