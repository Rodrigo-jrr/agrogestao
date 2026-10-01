-- Pecuária, entrega 1: lotes, animais, compra/venda de animais e eventos (linha do tempo).

-- Lote: INDIVIDUAL reúne animais com brinco; AGGREGATE guarda só a contagem de cabeças (aves, peixes,
-- ou bezerros manejados em grupo).
CREATE TABLE animal_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(20) NOT NULL,
    tracking VARCHAR(12) NOT NULL,
    head_count INT NOT NULL DEFAULT 0,
    activity_id BIGINT NULL,
    notes VARCHAR(500) NULL,
    archived_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_batch_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_batch_activity FOREIGN KEY (activity_id) REFERENCES activities(id),
    CONSTRAINT uk_batch_property_name UNIQUE (property_id, name)
);

-- Compra ou venda de animais (como purchases/sales, mas sem itens de estoque).
CREATE TABLE livestock_trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    kind VARCHAR(10) NOT NULL,
    supplier_id BIGINT NULL,
    customer_id BIGINT NULL,
    batch_id BIGINT NULL,
    trade_date DATE NOT NULL,
    head_count INT NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    payment_type VARCHAR(20) NOT NULL DEFAULT 'CASH',
    installment_count INT NOT NULL DEFAULT 0,
    financial_transaction_id BIGINT NULL,
    notes VARCHAR(500) NULL,
    canceled_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_trade_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_trade_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_trade_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_trade_batch FOREIGN KEY (batch_id) REFERENCES animal_batches(id),
    CONSTRAINT fk_trade_transaction FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions(id)
);

CREATE TABLE animals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    tag VARCHAR(30) NOT NULL,
    name VARCHAR(100) NULL,
    species VARCHAR(20) NOT NULL,
    breed VARCHAR(60) NULL,
    sex VARCHAR(10) NOT NULL,
    birth_date DATE NULL,
    birth_date_estimated BOOLEAN NOT NULL DEFAULT FALSE,
    origin VARCHAR(12) NOT NULL,
    entry_date DATE NOT NULL,
    mother_id BIGINT NULL,
    sire VARCHAR(100) NULL,
    batch_id BIGINT NULL,
    status VARCHAR(12) NOT NULL,
    exit_date DATE NULL,
    exit_reason VARCHAR(255) NULL,
    current_weight DECIMAL(10,2) NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_animal_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_animal_mother FOREIGN KEY (mother_id) REFERENCES animals(id),
    CONSTRAINT fk_animal_batch FOREIGN KEY (batch_id) REFERENCES animal_batches(id)
);

CREATE INDEX idx_animals_property_status ON animals(property_id, status);
CREATE INDEX idx_animals_property_tag ON animals(property_id, tag);

-- Linha do tempo: um evento por animal (animal_id) ou por lote inteiro (batch_id + head_count).
CREATE TABLE animal_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    animal_id BIGINT NULL,
    batch_id BIGINT NULL,
    type VARCHAR(30) NOT NULL,
    event_date DATE NOT NULL,
    head_count INT NULL,
    weight DECIMAL(10,2) NULL,
    from_batch_id BIGINT NULL,
    to_batch_id BIGINT NULL,
    trade_id BIGINT NULL,
    amount DECIMAL(15,2) NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_event_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_event_animal FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT fk_event_batch FOREIGN KEY (batch_id) REFERENCES animal_batches(id),
    CONSTRAINT fk_event_from_batch FOREIGN KEY (from_batch_id) REFERENCES animal_batches(id),
    CONSTRAINT fk_event_to_batch FOREIGN KEY (to_batch_id) REFERENCES animal_batches(id),
    CONSTRAINT fk_event_trade FOREIGN KEY (trade_id) REFERENCES livestock_trades(id)
);

CREATE INDEX idx_animal_events_animal ON animal_events(animal_id, event_date);
CREATE INDEX idx_animal_events_batch ON animal_events(batch_id, event_date);

-- Parcelas de compra/venda de animais a prazo.
ALTER TABLE accounts_payable ADD COLUMN livestock_trade_id BIGINT NULL;
ALTER TABLE accounts_payable ADD CONSTRAINT fk_payable_livestock_trade
    FOREIGN KEY (livestock_trade_id) REFERENCES livestock_trades(id);
ALTER TABLE accounts_receivable ADD COLUMN livestock_trade_id BIGINT NULL;
ALTER TABLE accounts_receivable ADD CONSTRAINT fk_receivable_livestock_trade
    FOREIGN KEY (livestock_trade_id) REFERENCES livestock_trades(id);
