-- Users
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

-- Workflows
CREATE TABLE workflows (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           name VARCHAR(255) NOT NULL,
                           description TEXT,
                           definition JSONB NOT NULL,       -- { trigger, conditions, actions }
                           is_active BOOLEAN NOT NULL DEFAULT false,
                           created_at TIMESTAMP NOT NULL DEFAULT now(),
                           updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_workflows_user ON workflows(user_id);
CREATE INDEX idx_workflows_active ON workflows(is_active) WHERE is_active = true;

-- Executions
CREATE TABLE workflow_executions (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
                                     status VARCHAR(20) NOT NULL,     -- PENDING, SUCCESS, FAILED
                                     trigger_payload JSONB,
                                     execution_log JSONB,
                                     error_message TEXT,
                                     started_at TIMESTAMP NOT NULL DEFAULT now(),
                                     finished_at TIMESTAMP,
                                     duration_ms INTEGER
);

CREATE INDEX idx_exec_workflow ON workflow_executions(workflow_id);
CREATE INDEX idx_exec_started ON workflow_executions(started_at DESC);