-- Vincula compras/vendas ao lançamento financeiro gerado e permite cancelamento (com estorno).
ALTER TABLE purchases ADD COLUMN financial_transaction_id BIGINT NULL;
ALTER TABLE purchases ADD COLUMN canceled_at DATETIME NULL;
ALTER TABLE purchases ADD CONSTRAINT fk_purchase_transaction
    FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions(id);

ALTER TABLE sales ADD COLUMN financial_transaction_id BIGINT NULL;
ALTER TABLE sales ADD COLUMN canceled_at DATETIME NULL;
ALTER TABLE sales ADD CONSTRAINT fk_sale_transaction
    FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions(id);

-- Registros já existentes: o lançamento foi criado com a descrição "Compra #id" / "Venda #id".
UPDATE purchases SET financial_transaction_id = (
    SELECT MIN(t.id) FROM financial_transactions t
    WHERE t.property_id = purchases.property_id AND t.description = CONCAT('Compra #', purchases.id));

UPDATE sales SET financial_transaction_id = (
    SELECT MIN(t.id) FROM financial_transactions t
    WHERE t.property_id = sales.property_id AND t.description = CONCAT('Venda #', sales.id));
