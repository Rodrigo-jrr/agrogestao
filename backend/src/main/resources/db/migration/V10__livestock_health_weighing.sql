-- Pecuária, entrega 2: pesagens e sanidade (vacinas, medicamentos, procedimentos).

-- Uma aplicação sanitária: um produto (do estoque ou só o nome) aplicado num animal, em vários ou num lote.
-- Cada animal atingido ganha um evento SANIDADE ligado a ela (animal_events.application_id).
CREATE TABLE health_applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    application_date DATE NOT NULL,
    batch_id BIGINT NULL,
    head_count INT NOT NULL,
    product_id BIGINT NULL,
    product_name VARCHAR(150) NOT NULL,
    dose DECIMAL(12,3) NULL,
    dose_unit VARCHAR(20) NULL,
    total_quantity DECIMAL(12,3) NULL,
    total_cost DECIMAL(15,2) NULL,
    withdrawal_days INT NULL,
    withdrawal_until DATE NULL,
    next_date DATE NULL,
    next_description VARCHAR(255) NULL,
    follow_up_done_at DATETIME NULL,
    responsible VARCHAR(100) NULL,
    notes VARCHAR(500) NULL,
    inventory_movement_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_health_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_health_batch FOREIGN KEY (batch_id) REFERENCES animal_batches(id),
    CONSTRAINT fk_health_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_health_movement FOREIGN KEY (inventory_movement_id) REFERENCES inventory_movements(id)
);

CREATE INDEX idx_health_property_next ON health_applications(property_id, next_date);
CREATE INDEX idx_health_property_date ON health_applications(property_id, application_date);

ALTER TABLE animal_events ADD COLUMN application_id BIGINT NULL;
ALTER TABLE animal_events ADD CONSTRAINT fk_event_application
    FOREIGN KEY (application_id) REFERENCES health_applications(id);
-- Carência do animal por causa desta aplicação (consultas de "em carência" sem juntar tabelas).
ALTER TABLE animal_events ADD COLUMN withdrawal_until DATE NULL;
CREATE INDEX idx_animal_events_withdrawal ON animal_events(property_id, withdrawal_until);
