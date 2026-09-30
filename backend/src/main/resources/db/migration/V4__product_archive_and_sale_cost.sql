-- Produtos arquivados somem das listas e formulários, mas mantêm o histórico.
ALTER TABLE products ADD COLUMN archived_at DATETIME NULL;

-- Custo (médio) de cada item no momento da venda: permite estornar o estoque pelo custo certo
-- ao cancelar a venda e calcular a margem.
ALTER TABLE sale_items ADD COLUMN unit_cost DECIMAL(15,2) NULL;

CREATE INDEX idx_movements_product ON inventory_movements(product_id, movement_date);
