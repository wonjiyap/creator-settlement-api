CREATE TABLE fee_rate_history (
    id              VARCHAR(64)   NOT NULL PRIMARY KEY,
    -- 이 연월("YYYY-MM")부터 새 수수료율 적용 (다음 변경 전까지 유효)
    effective_month VARCHAR(7)    NOT NULL,
    fee_rate        DECIMAL(5, 4) NOT NULL,
    created_at      TIMESTAMP     NOT NULL
);

-- 같은 적용 시작 월에 두 개의 율이 존재할 수 없다
ALTER TABLE fee_rate_history ADD CONSTRAINT uk_fee_rate_effective_month UNIQUE (effective_month);
