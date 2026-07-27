CREATE TABLE IF NOT EXISTS expense_report (
    session_id VARCHAR(32) PRIMARY KEY,
    app_user_id BIGINT NOT NULL REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_expense_report_app_user_id
    ON expense_report(app_user_id);
