CREATE TABLE integrations (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              type VARCHAR(50) NOT NULL,               -- SLACK, SMTP, WEBHOOK
                              name VARCHAR(255) NOT NULL,              -- "Slack da empresa"
                              config JSONB NOT NULL,                   -- dados sensíveis (criptografar em produção)
                              is_active BOOLEAN NOT NULL DEFAULT true,
                              created_at TIMESTAMP NOT NULL DEFAULT now(),
                              updated_at TIMESTAMP NOT NULL DEFAULT now(),
                              UNIQUE(user_id, type, name)
);

CREATE INDEX idx_integrations_user ON integrations(user_id);