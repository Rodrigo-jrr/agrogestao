-- Agricultura, entrega 4: colheitas (uma safra pode ter várias, os indicadores somam tudo).
-- destination é lista fixa do produto (ESTOQUE ou VENDA), não catálogo do cliente, por isso VARCHAR(20)
-- sem tabela própria (mesmo tratamento de crop_operations.kind, V16).
-- Campos condicionais por destino ficam NULL quando não se aplicam; obrigatoriedade é regra do Back End.
CREATE TABLE crop_harvests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    crop_cycle_id BIGINT NOT NULL,
    harvest_date DATE NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    quantity_unit VARCHAR(20) NOT NULL,
    destination VARCHAR(20) NOT NULL,
    -- ESTOQUE: entra como item de produto já cadastrado, pelo custo médio do estoque.
    product_id BIGINT NULL,
    unit_cost DECIMAL(15,2) NULL,
    inventory_movement_id BIGINT NULL,
    -- VENDA: não passa por Vendas (sem baixa de estoque) — receita direto, mesmo padrão de livestock_trades (V9).
    customer_id BIGINT NULL,
    total_amount DECIMAL(15,2) NULL,
    payment_type VARCHAR(20) NULL,
    financial_transaction_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_crop_harvest_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_crop_harvest_cycle FOREIGN KEY (crop_cycle_id) REFERENCES crop_cycles(id),
    CONSTRAINT fk_crop_harvest_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_crop_harvest_movement FOREIGN KEY (inventory_movement_id) REFERENCES inventory_movements(id),
    CONSTRAINT fk_crop_harvest_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_crop_harvest_transaction FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions(id)
);

CREATE INDEX idx_crop_harvests_property ON crop_harvests(property_id);
CREATE INDEX idx_crop_harvests_cycle ON crop_harvests(crop_cycle_id);
CREATE INDEX idx_crop_harvests_property_date ON crop_harvests(property_id, harvest_date);

-- Parcelas de colheita vendida a prazo (mesmo padrão de livestock_trade_id em accounts_receivable, V9).
ALTER TABLE accounts_receivable ADD COLUMN crop_harvest_id BIGINT NULL;
ALTER TABLE accounts_receivable ADD CONSTRAINT fk_receivable_crop_harvest
    FOREIGN KEY (crop_harvest_id) REFERENCES crop_harvests(id);
