CREATE TABLE settlement (
    id                VARCHAR(64)   NOT NULL PRIMARY KEY,
    creator_id        VARCHAR(64)   NOT NULL,
    -- 정산 대상 연월 "YYYY-MM" (H2에서 MONTH는 예약어라 period 사용)
    period            VARCHAR(7)    NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    total_sales       BIGINT        NOT NULL,
    total_refund      BIGINT        NOT NULL,
    net_sales         BIGINT        NOT NULL,
    -- 생성 당시 수수료율 스냅샷 (수수료율 변경 이력 확장 대비)
    fee_rate          DECIMAL(5, 4) NOT NULL,
    fee               BIGINT        NOT NULL,
    settlement_amount BIGINT        NOT NULL,
    sale_count        BIGINT        NOT NULL,
    cancel_count      BIGINT        NOT NULL,
    created_at        TIMESTAMP     NOT NULL,
    confirmed_at      TIMESTAMP,
    paid_at           TIMESTAMP
);

-- 동일 크리에이터 + 동일 월 중복 정산 방지 (creator_id 선두 복합 인덱스 겸용)
ALTER TABLE settlement ADD CONSTRAINT uk_settlement_creator_period UNIQUE (creator_id, period);

-- 목록 조회 단독 필터용 (period·status는 유니크 인덱스의 선두 컬럼이 아님)
CREATE INDEX idx_settlement_period ON settlement (period);
CREATE INDEX idx_settlement_status ON settlement (status);

