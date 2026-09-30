-- Compras e vendas a prazo: cada parcela vira uma conta a pagar/receber ligada à operação.
ALTER TABLE purchases ADD COLUMN payment_type VARCHAR(20) NOT NULL DEFAULT 'CASH';
ALTER TABLE purchases ADD COLUMN installment_count INT NOT NULL DEFAULT 0;
ALTER TABLE sales ADD COLUMN payment_type VARCHAR(20) NOT NULL DEFAULT 'CASH';
ALTER TABLE sales ADD COLUMN installment_count INT NOT NULL DEFAULT 0;

ALTER TABLE accounts_payable ADD COLUMN purchase_id BIGINT NULL;
ALTER TABLE accounts_payable ADD COLUMN installment_number INT NULL;
ALTER TABLE accounts_payable ADD COLUMN installment_count INT NULL;
ALTER TABLE accounts_payable ADD CONSTRAINT fk_payable_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id);

ALTER TABLE accounts_receivable ADD COLUMN sale_id BIGINT NULL;
ALTER TABLE accounts_receivable ADD COLUMN installment_number INT NULL;
ALTER TABLE accounts_receivable ADD COLUMN installment_count INT NULL;
ALTER TABLE accounts_receivable ADD CONSTRAINT fk_receivable_sale FOREIGN KEY (sale_id) REFERENCES sales(id);
