-- Agricultura, entrega 1: talhões (cadastro básico; cultura/safra e indicadores vêm nas próximas entregas).
CREATE TABLE fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    area_ha DECIMAL(12,2) NOT NULL,
    location VARCHAR(255) NULL,
    soil_type VARCHAR(100) NULL,
    -- Nenhuma safra referencia talhão ainda; arquivar (em vez de excluir) já deixa a porta aberta pra quando
    -- isso passar a importar, no mesmo padrão de products/animal_batches.archived_at.
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_field_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE INDEX idx_fields_property ON fields(property_id);
