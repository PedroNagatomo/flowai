ALTER TABLE workflows
    ADD COLUMN last_run_at TIMESTAMP,
    ADD COLUMN next_run_at TIMESTAMP;

CREATE INDEX idx_workflows_next_run ON workflows(next_run_at)
    WHERE is_active = true AND next_run_at IS NOT NULL;