-- Agricultura, entrega 2: catálogo de culturas e safras (ciclos de cultivo).
-- Cultura segue o mesmo padrão do catálogo de espécies (V11): cadastro livre por propriedade, sem enum fixo
-- do sistema. Variedade fica como texto livre (não é catálogo) — mesma decisão já tomada para a máquina usada
-- nas operações agrícolas (entrega 3, ainda não implementada).

CREATE TABLE crops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_crop_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE INDEX idx_crops_property ON crops(property_id);

-- Safra/ciclo: talhão + cultura (referências reais) e variedade em texto livre. Sem exclusão física, mesmo
-- padrão de fields (entrega 1): só arquivar.
CREATE TABLE crop_cycles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    field_id BIGINT NOT NULL,
    crop_id BIGINT NOT NULL,
    variety VARCHAR(100) NULL,
    planned_planting_date DATE NOT NULL,
    planned_harvest_date DATE NULL,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_crop_cycle_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_crop_cycle_field FOREIGN KEY (field_id) REFERENCES fields(id),
    CONSTRAINT fk_crop_cycle_crop FOREIGN KEY (crop_id) REFERENCES crops(id)
);

CREATE INDEX idx_crop_cycles_property ON crop_cycles(property_id);
CREATE INDEX idx_crop_cycles_field ON crop_cycles(field_id);
CREATE INDEX idx_crop_cycles_crop ON crop_cycles(crop_id);
