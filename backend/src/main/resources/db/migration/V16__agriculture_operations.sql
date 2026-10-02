-- Agricultura, entrega 3: operações agrícolas (preparo, plantio, adubação, pulverização).
-- Mesmo padrão de health_applications (V10): evento com insumo opcional do estoque, custo e responsável.
-- kind é lista fixa do produto (não varia por cliente), por isso enum Java + VARCHAR, sem tabela de catálogo.
CREATE TABLE crop_operations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    crop_cycle_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    operation_date DATE NOT NULL,
    product_id BIGINT NULL,
    product_name VARCHAR(150) NULL,
    quantity DECIMAL(12,3) NULL,
    quantity_unit VARCHAR(20) NULL,
    total_cost DECIMAL(15,2) NULL,
    responsible VARCHAR(100) NULL,
    machine VARCHAR(150) NULL,
    notes VARCHAR(500) NULL,
    inventory_movement_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_crop_operation_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_crop_operation_cycle FOREIGN KEY (crop_cycle_id) REFERENCES crop_cycles(id),
    CONSTRAINT fk_crop_operation_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_crop_operation_movement FOREIGN KEY (inventory_movement_id) REFERENCES inventory_movements(id)
);

CREATE INDEX idx_crop_operations_property ON crop_operations(property_id);
CREATE INDEX idx_crop_operations_cycle ON crop_operations(crop_cycle_id);
CREATE INDEX idx_crop_operations_property_date ON crop_operations(property_id, operation_date);
