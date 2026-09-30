-- Troca de e-mail: o endereço novo fica pendente até ser confirmado pelo link enviado a ele.
ALTER TABLE users ADD COLUMN pending_email VARCHAR(180) NULL;

-- Convites para quem ainda não tem conta. Como em user_tokens, guardamos só o hash SHA-256 do token.
CREATE TABLE property_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    invited_by BIGINT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_invitation_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_invitation_inviter FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_invitation_token UNIQUE (token_hash),
    CONSTRAINT uk_invitation_property_email UNIQUE (property_id, email)
);

-- Tela de histórico: filtra por propriedade e período.
CREATE INDEX idx_audit_logs_property_created ON audit_logs(property_id, created_at);
