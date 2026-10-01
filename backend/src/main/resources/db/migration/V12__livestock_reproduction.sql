-- Pecuária, entrega 3: reprodução. Mesmo desenho da sanidade (V10): um novo AnimalEventType (REPRODUCAO) na
-- linha do tempo do animal, com os detalhes de cada evento numa tabela própria ligada por animal_events.reproductive_event_id.
-- Situação reprodutiva (vazia/coberta/prenhe) e histórico são derivados desta linha do tempo, não guardados à parte.

CREATE TABLE reproductive_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    -- Sempre a fêmea: reprodução só existe para animais com brinco (species.allows_individual = true).
    animal_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    event_date DATE NOT NULL,
    -- Touro/reprodutor (cobertura) ou identificação do sêmen (inseminação); texto livre, como animals.sire.
    sire VARCHAR(100) NULL,
    -- Resultado do diagnóstico de prenhez.
    pregnancy_result VARCHAR(10) NULL,
    -- Previsão do parto, preenchida quando o diagnóstico é positivo; alimenta o alerta de partos próximos.
    expected_calving_date DATE NULL,
    -- No parto/aborto, o diagnóstico de prenhez que esta gestação encerra (obrigatório: diagnóstico precede o
    -- parto). Permite derivar a situação atual da fêmea sem duplicar estado.
    diagnosis_event_id BIGINT NULL,
    -- No parto: quantas crias nasceram (os cadastros das crias em si ficam em animals, como hoje).
    offspring_count INT NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_repro_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_repro_animal FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT fk_repro_diagnosis FOREIGN KEY (diagnosis_event_id) REFERENCES reproductive_events(id)
);

CREATE INDEX idx_repro_property_date ON reproductive_events(property_id, event_date);
CREATE INDEX idx_repro_animal ON reproductive_events(animal_id, event_date);
-- Fêmeas com diagnóstico positivo e previsão de parto nos próximos dias (alerta de parto previsto).
CREATE INDEX idx_repro_expected_calving ON reproductive_events(property_id, expected_calving_date);

ALTER TABLE animal_events ADD COLUMN reproductive_event_id BIGINT NULL;
ALTER TABLE animal_events ADD CONSTRAINT fk_event_reproductive
    FOREIGN KEY (reproductive_event_id) REFERENCES reproductive_events(id);
