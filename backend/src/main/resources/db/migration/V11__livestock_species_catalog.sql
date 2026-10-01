-- Pecuária, pré-requisito da entrega 3: catálogo de espécies por propriedade, no lugar do enum fixo Species.
-- Cada propriedade cadastra livremente as espécies que usa e diz se cada uma admite brinco (cadastro
-- individual) ou só contagem — a mesma escolha que antes vinha fixa no enum. O backend ainda usa o enum
-- Species temporariamente (ver AttributeConverter); a reescrita completa fica para a próxima entrega.

CREATE TABLE species (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    allows_individual BOOLEAN NOT NULL DEFAULT TRUE,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_species_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE INDEX idx_species_property ON species(property_id);

-- Semente: uma linha por (propriedade, espécie) já em uso em animals/animal_batches, reaproveitando o
-- rótulo e o allowsIndividual que o enum Species tinha para esse valor.
INSERT INTO species (property_id, name, allows_individual, created_at, updated_at)
SELECT DISTINCT property_id,
    CASE species
        WHEN 'BOVINO' THEN 'Bovino'
        WHEN 'BUBALINO' THEN 'Bubalino'
        WHEN 'OVINO' THEN 'Ovino'
        WHEN 'CAPRINO' THEN 'Caprino'
        WHEN 'SUINO' THEN 'Suíno'
        WHEN 'EQUINO' THEN 'Equino'
        WHEN 'AVE' THEN 'Ave'
        WHEN 'PEIXE' THEN 'Peixe'
        ELSE 'Outro'
    END,
    CASE species
        WHEN 'AVE' THEN FALSE
        WHEN 'PEIXE' THEN FALSE
        ELSE TRUE
    END,
    NOW(), NOW()
FROM (
    SELECT property_id, species FROM animals
    UNION
    SELECT property_id, species FROM animal_batches
) combos;

-- animals: troca species (texto do enum) por species_id (FK para o catálogo da propriedade).
ALTER TABLE animals ADD COLUMN species_id BIGINT NULL;
UPDATE animals SET species_id = (
    SELECT s.id FROM species s
    WHERE s.property_id = animals.property_id
      AND s.name = CASE animals.species
            WHEN 'BOVINO' THEN 'Bovino'
            WHEN 'BUBALINO' THEN 'Bubalino'
            WHEN 'OVINO' THEN 'Ovino'
            WHEN 'CAPRINO' THEN 'Caprino'
            WHEN 'SUINO' THEN 'Suíno'
            WHEN 'EQUINO' THEN 'Equino'
            WHEN 'AVE' THEN 'Ave'
            WHEN 'PEIXE' THEN 'Peixe'
            ELSE 'Outro'
          END
    LIMIT 1
);
ALTER TABLE animals MODIFY COLUMN species_id BIGINT NOT NULL;
ALTER TABLE animals ADD CONSTRAINT fk_animal_species FOREIGN KEY (species_id) REFERENCES species(id);
ALTER TABLE animals DROP COLUMN species;
CREATE INDEX idx_animals_species ON animals(species_id);

-- animal_batches: idem.
ALTER TABLE animal_batches ADD COLUMN species_id BIGINT NULL;
UPDATE animal_batches SET species_id = (
    SELECT s.id FROM species s
    WHERE s.property_id = animal_batches.property_id
      AND s.name = CASE animal_batches.species
            WHEN 'BOVINO' THEN 'Bovino'
            WHEN 'BUBALINO' THEN 'Bubalino'
            WHEN 'OVINO' THEN 'Ovino'
            WHEN 'CAPRINO' THEN 'Caprino'
            WHEN 'SUINO' THEN 'Suíno'
            WHEN 'EQUINO' THEN 'Equino'
            WHEN 'AVE' THEN 'Ave'
            WHEN 'PEIXE' THEN 'Peixe'
            ELSE 'Outro'
          END
    LIMIT 1
);
ALTER TABLE animal_batches MODIFY COLUMN species_id BIGINT NOT NULL;
ALTER TABLE animal_batches ADD CONSTRAINT fk_batch_species FOREIGN KEY (species_id) REFERENCES species(id);
ALTER TABLE animal_batches DROP COLUMN species;
CREATE INDEX idx_batches_species ON animal_batches(species_id);
