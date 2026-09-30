-- Verificação de e-mail e recuperação de senha.
ALTER TABLE users ADD COLUMN email_verified_at DATETIME NULL;
-- Tokens JWT emitidos antes desta data são rejeitados (ex.: após redefinir a senha).
ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;

-- Links de uso único enviados por e-mail. Guardamos só o hash SHA-256 do token, nunca o token em si.
CREATE TABLE user_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_user_token_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_user_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_user_tokens_user_purpose ON user_tokens(user_id, purpose, created_at);
