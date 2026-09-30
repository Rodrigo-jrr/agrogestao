-- Aceite dos termos de uso e da política de privacidade (LGPD): qual versão, quando e de onde.
-- Um registro por aceite, para manter o histórico quando os termos mudarem.
CREATE TABLE terms_acceptances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    terms_version VARCHAR(20) NOT NULL,
    accepted_at DATETIME NOT NULL,
    ip_address VARCHAR(45) NULL,
    CONSTRAINT fk_terms_acceptance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_terms_acceptances_user ON terms_acceptances(user_id, terms_version);
