-- No FK: failures must survive rollback of a newly created report.
-- Metadata is an allowlisted collection of structural counts, never source data.
CREATE TABLE app_error (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(64) NOT NULL,
    stage varchar(32) NOT NULL,
    report_id varchar(32),
    correlation_id uuid NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    fingerprint varchar(64) NOT NULL UNIQUE,
    first_seen timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    count bigint NOT NULL DEFAULT 1 CHECK (count > 0)
);
