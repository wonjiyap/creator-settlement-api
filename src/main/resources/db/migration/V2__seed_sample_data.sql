-- 샘플 데이터 시딩 (인메모리 H2 → 매 부팅 재적재)

-- =========================================================================
-- Creators
-- =========================================================================
INSERT INTO creator (id, name) VALUES
  ('creator-1', '김강사'),
  ('creator-2', '이강사'),
  ('creator-3', '박강사'),
  ('creator-4', '최강사'),
  ('creator-5', '정강사');

-- =========================================================================
-- Courses
-- =========================================================================
INSERT INTO course (id, creator_id, title) VALUES
  ('course-1',  'creator-1', 'Spring Boot 입문'),
  ('course-2',  'creator-1', 'JPA 실전'),
  ('course-3',  'creator-2', 'Kotlin 기초'),
  ('course-4',  'creator-3', 'MSA 설계'),
  ('course-5',  'creator-2', '코루틴 심화'),
  ('course-6',  'creator-3', '쿠버네티스 운영'),
  ('course-7',  'creator-4', 'React 입문'),
  ('course-8',  'creator-4', 'TypeScript 실전'),
  ('course-9',  'creator-5', '데이터 분석 기초'),
  ('course-10', 'creator-1', '테스트 자동화');

-- =========================================================================
-- [명세 보존] 판매 내역 — creator-1 2025-03 시나리오 + 월 경계 + creator-3 빈 월
--   creator-1 2025-03: 총 260,000 / 환불 110,000 / 순 150,000 / 수수료 30,000 / 정산 120,000
-- =========================================================================
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-1', 'course-1', 'student-1',  50000,  '2025-03-05 10:00:00'),
  ('sale-2', 'course-1', 'student-2',  50000,  '2025-03-15 14:30:00'),
  ('sale-3', 'course-2', 'student-3',  80000,  '2025-03-20 09:00:00'),
  ('sale-4', 'course-2', 'student-4',  80000,  '2025-03-22 11:00:00'),
  ('sale-5', 'course-3', 'student-5',  60000,  '2025-01-31 23:30:00'),
  ('sale-6', 'course-3', 'student-6',  60000,  '2025-03-10 16:00:00'),
  ('sale-7', 'course-4', 'student-7', 120000,  '2025-02-14 10:00:00');

-- [명세 보존] 취소 내역
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-1', 'sale-3', 80000, '2025-03-25 10:00:00'),  -- 전액 환불(3월)
  ('cancel-2', 'sale-4', 30000, '2025-03-26 10:00:00'),  -- 부분 환불(3월, 30,000 < 80,000)
  ('cancel-3', 'sale-5', 60000, '2025-02-02 09:00:00');  -- 월 경계: 1월 판매 → 2월 취소

-- =========================================================================
-- [추가] 다양한 엣지케이스용 데이터 (명세 시나리오와 겹치지 않는 크리에이터/월만 사용)
-- =========================================================================

-- creator-4 / 2025-03 : 정상 + 전액환불(동월) + 부분환불(동일자) + 한 판매에 다수 부분취소
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-101', 'course-7', 'student-8',   70000, '2025-03-02 09:00:00'),
  ('sale-102', 'course-7', 'student-9',   70000, '2025-03-11 13:00:00'),
  ('sale-103', 'course-8', 'student-10',  90000, '2025-03-18 15:00:00'),
  ('sale-104', 'course-8', 'student-11',  90000, '2025-03-28 20:00:00'),
  ('sale-105', 'course-8', 'student-12', 120000, '2025-03-08 10:00:00');
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-101', 'sale-101', 70000, '2025-03-05 10:00:00'),  -- 전액 환불(동월)
  ('cancel-102', 'sale-103', 40000, '2025-03-18 16:00:00'),  -- 부분 환불(판매와 같은 날)
  ('cancel-103', 'sale-105', 30000, '2025-03-09 10:00:00'),  -- 다수 부분취소 ①
  ('cancel-104', 'sale-105', 20000, '2025-03-15 10:00:00');  -- 다수 부분취소 ② (누적 50,000 < 120,000)

-- creator-4 / 2025-04~05 : 월 시작·끝 경계 시각 + 크로스먼스 전액환불(→ 5월 순매출 음수)
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-106', 'course-7', 'student-13', 70000, '2025-04-01 00:00:00'),  -- 월 시작 경계
  ('sale-107', 'course-8', 'student-14', 90000, '2025-04-30 23:59:59');  -- 월 끝 경계
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-105', 'sale-106', 70000, '2025-05-01 00:00:00');  -- 4월 판매 → 5월 취소(5월은 판매 0 → 순매출 -70,000)

-- creator-5 / 2025-05~06 : 재구매(동일 수강생, 다른 월) + 동월 전액환불
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-108', 'course-9', 'student-15', 150000, '2025-05-10 10:00:00'),
  ('sale-109', 'course-9', 'student-16', 150000, '2025-05-20 10:00:00'),
  ('sale-110', 'course-9', 'student-15', 150000, '2025-06-03 10:00:00');  -- student-15 재구매
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-106', 'sale-109', 150000, '2025-05-25 10:00:00');  -- 동월 전액환불

-- creator-1 / 2025-03 외 다른 월·전년도 (3월 시나리오 불변 유지)
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-111', 'course-1',  'student-17', 50000, '2025-04-05 10:00:00'),
  ('sale-112', 'course-2',  'student-18', 80000, '2025-04-15 10:00:00'),
  ('sale-113', 'course-10', 'student-19', 60000, '2025-05-05 10:00:00'),
  ('sale-114', 'course-10', 'student-20', 60000, '2025-05-06 10:00:00'),
  ('sale-115', 'course-1',  'student-21', 50000, '2024-12-20 10:00:00');  -- 전년도(2024)
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-107', 'sale-112', 80000, '2025-04-20 10:00:00');  -- 4월 전액환불

-- creator-2 / 1·2월 외 다른 월 (월 경계 시나리오 불변 유지)
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-116', 'course-5', 'student-22', 100000, '2025-04-10 10:00:00'),
  ('sale-117', 'course-5', 'student-23', 100000, '2025-04-12 10:00:00'),
  ('sale-118', 'course-3', 'student-24',  60000, '2025-05-01 09:00:00');
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-108', 'sale-116', 50000, '2025-04-13 10:00:00');  -- 부분 환불

-- creator-3 / 2025-03 외 다른 월·전년도 (3월 '빈 월' 시나리오 불변 유지)
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-119', 'course-6', 'student-25', 110000, '2025-04-08 10:00:00'),
  ('sale-120', 'course-6', 'student-26', 110000, '2025-04-09 10:00:00'),
  ('sale-121', 'course-4', 'student-27', 120000, '2024-11-15 10:00:00');  -- 전년도(2024)
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-109', 'sale-119', 110000, '2025-04-25 10:00:00');  -- 4월 전액환불

-- creator-4 / 2025-06 : 다건 판매(볼륨) + 전액/부분 환불 혼재
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
  ('sale-122', 'course-7', 'student-28', 70000, '2025-06-05 10:00:00'),
  ('sale-123', 'course-7', 'student-29', 70000, '2025-06-06 10:00:00'),
  ('sale-124', 'course-8', 'student-30', 90000, '2025-06-07 10:00:00'),
  ('sale-125', 'course-8', 'student-8',  90000, '2025-06-08 10:00:00');  -- student-8: course-7(sale-101) 구매 이력 있는 수강생이 다른 강의 구매
INSERT INTO cancel_record (id, sale_record_id, refund_amount, canceled_at) VALUES
  ('cancel-110', 'sale-124', 90000, '2025-06-10 10:00:00'),  -- 전액환불
  ('cancel-111', 'sale-125', 45000, '2025-06-11 10:00:00');  -- 부분환불
