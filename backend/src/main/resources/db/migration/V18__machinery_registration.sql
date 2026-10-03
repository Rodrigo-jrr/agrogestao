-- Máquinas e manutenção, entrega 1: cadastro de máquinas e implementos.
-- Tipo de máquina é catálogo livre por propriedade, mesmo modelo de crops/species (V15/V11).
CREATE TABLE machine_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_machine_type_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE INDEX idx_machine_types_property ON machine_types(property_id);

-- measurement_type é lista fixa do produto (horímetro ou hodômetro), não catálogo do cliente — mesmo
-- tratamento de crop_operations.kind (V16). current_reading guarda a leitura atual (horas ou km, conforme
-- o tipo) para a manutenção preventiva por horas/km que vem numa entrega futura.
-- Sem referência ainda a machines (vem na entrega 2, com operações agrícolas); por isso ainda não há regra
-- de "só arquivar se usada" no banco — exclusão física livre por agora.
CREATE TABLE machines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    machine_type_id BIGINT NOT NULL,
    identification VARCHAR(60) NULL,
    measurement_type VARCHAR(20) NOT NULL,
    current_reading DECIMAL(12,2) NOT NULL DEFAULT 0,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_machine_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_machine_type FOREIGN KEY (machine_type_id) REFERENCES machine_types(id)
);

CREATE INDEX idx_machines_property ON machines(property_id);
