CREATE TABLE creator (
    id   VARCHAR(64)  NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE course (
    id         VARCHAR(64)  NOT NULL PRIMARY KEY,
    creator_id VARCHAR(64)  NOT NULL,
    title      VARCHAR(255) NOT NULL
);

CREATE TABLE sale_record (
    id         VARCHAR(64)              NOT NULL PRIMARY KEY,
    course_id  VARCHAR(64)              NOT NULL,
    student_id VARCHAR(64)              NOT NULL,
    amount     BIGINT                   NOT NULL,
    paid_at    TIMESTAMP    NOT NULL
);

CREATE TABLE cancel_record (
    id             VARCHAR(64)              NOT NULL PRIMARY KEY,
    sale_record_id VARCHAR(64)              NOT NULL,
    refund_amount  BIGINT                   NOT NULL,
    canceled_at    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_course_creator ON course (creator_id);
CREATE INDEX idx_sale_course ON sale_record (course_id);
CREATE INDEX idx_sale_paid_at ON sale_record (paid_at);
CREATE INDEX idx_cancel_sale ON cancel_record (sale_record_id);
CREATE INDEX idx_cancel_canceled_at ON cancel_record (canceled_at);
