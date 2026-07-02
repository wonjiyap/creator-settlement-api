-- 정산 스냅샷 시드 (V2 판매/취소 데이터 기준으로 계산한 값 그대로)
--
-- 주의: 동일 크리에이터+월 중복 정산은 409로 막히므로, 테스트가 create() 하는
-- 크리에이터/월(creator-1 2025-03, creator-2 2025-01, creator-3 2025-03,
-- creator-4 2025-05·06, creator-5 2025-06)은 시드에 넣지 않는다.
--
-- 상태 시나리오: 과거 월일수록 정산이 진행된 상태(PAID → CONFIRMED → PENDING).
-- 정산 생성일은 대상 월의 다음 달 1일로 가정.

INSERT INTO settlement (id, creator_id, period, status, total_sales, total_refund, net_sales, fee_rate, fee, settlement_amount, sale_count, cancel_count, created_at, confirmed_at, paid_at) VALUES
  -- creator-2 2025-02: 취소만 있는 월(cancel-3) → 순매출 음수, 지급 보류(PENDING)
  ('settlement-1', 'creator-2', '2025-02', 'PENDING',        0,  60000,  -60000, 0.2000, -12000,  -48000, 0, 1, '2025-03-01 10:00:00', NULL,                  NULL),
  -- creator-4 2025-03: sale-101~105 / cancel-101~104 → 지급 완료(PAID)
  ('settlement-2', 'creator-4', '2025-03', 'PAID',      440000, 160000,  280000, 0.2000,  56000,  224000, 5, 4, '2025-04-01 10:00:00', '2025-04-03 10:00:00', '2025-04-10 10:00:00'),
  -- creator-1 2025-04: sale-111,112 / cancel-107 → 지급 완료(PAID)
  ('settlement-3', 'creator-1', '2025-04', 'PAID',      130000,  80000,   50000, 0.2000,  10000,   40000, 2, 1, '2025-05-01 10:00:00', '2025-05-03 10:00:00', '2025-05-10 10:00:00'),
  -- creator-4 2025-04: sale-106,107(월 경계 시각) → 확정 후 지급 대기(CONFIRMED)
  ('settlement-4', 'creator-4', '2025-04', 'CONFIRMED', 160000,      0,  160000, 0.2000,  32000,  128000, 2, 0, '2025-05-01 10:00:00', '2025-05-03 10:00:00', NULL),
  -- creator-5 2025-05: sale-108,109 / cancel-106(동월 전액환불) → 확정 대기(PENDING)
  ('settlement-5', 'creator-5', '2025-05', 'PENDING',   300000, 150000,  150000, 0.2000,  30000,  120000, 2, 1, '2025-06-01 10:00:00', NULL,                  NULL);
