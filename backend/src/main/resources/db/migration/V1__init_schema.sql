CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE organization_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_org_member_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_org_member_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_org_member (organization_id, user_id)
);

CREATE TABLE properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255),
    total_area_ha DECIMAL(12,2),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_property_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE property_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_property_member_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_property_member_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_property_member (property_id, user_id)
);

CREATE TABLE activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(60) NOT NULL,
    area_ha DECIMAL(12,2),
    responsible VARCHAR(150),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_activity_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE TABLE cost_centers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    activity_id BIGINT,
    name VARCHAR(150) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_cost_center_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_cost_center_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
);

CREATE TABLE financial_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    cost_center_id BIGINT,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_transaction_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_transaction_cost_center FOREIGN KEY (cost_center_id) REFERENCES cost_centers(id)
);

CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    document VARCHAR(30),
    phone VARCHAR(30),
    email VARCHAR(180),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_supplier_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE TABLE customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    document VARCHAR(30),
    phone VARCHAR(30),
    email VARCHAR(180),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_customer_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE TABLE accounts_payable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    supplier_id BIGINT,
    financial_transaction_id BIGINT,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    due_date DATE NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_payable_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_payable_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_payable_transaction FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions(id)
);

CREATE TABLE accounts_receivable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    customer_id BIGINT,
    financial_transaction_id BIGINT,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    due_date DATE NOT NULL,
    received BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_receivable_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_receivable_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_receivable_transaction FOREIGN KEY (financial_transaction_id) REFERENCES financial_transactions(id)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(60),
    unit VARCHAR(20) NOT NULL,
    min_stock DECIMAL(15,3) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_product_property FOREIGN KEY (property_id) REFERENCES properties(id)
);

CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(15,3) NOT NULL DEFAULT 0,
    average_cost DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_inventory_item_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_inventory_item_product FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY uk_inventory_item (property_id, product_id)
);

CREATE TABLE inventory_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity DECIMAL(15,3) NOT NULL,
    unit_cost DECIMAL(15,2),
    movement_date DATE NOT NULL,
    notes VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_movement_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_movement_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    purchase_date DATE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_purchase_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE purchase_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(15,3) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_purchase_item_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id),
    CONSTRAINT fk_purchase_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    sale_date DATE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_sale_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE TABLE sale_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(15,3) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_sale_item_sale FOREIGN KEY (sale_id) REFERENCES sales(id),
    CONSTRAINT fk_sale_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE production_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    activity_id BIGINT,
    product_name VARCHAR(150) NOT NULL,
    quantity DECIMAL(15,3) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    production_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_production_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_production_activity FOREIGN KEY (activity_id) REFERENCES activities(id)
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT,
    user_id BIGINT,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    details VARCHAR(500),
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_activities_property ON activities(property_id);
CREATE INDEX idx_cost_centers_property ON cost_centers(property_id);
CREATE INDEX idx_transactions_property_date ON financial_transactions(property_id, transaction_date);
CREATE INDEX idx_payable_property_due ON accounts_payable(property_id, due_date);
CREATE INDEX idx_receivable_property_due ON accounts_receivable(property_id, due_date);
CREATE INDEX idx_products_property ON products(property_id);
CREATE INDEX idx_movements_property_date ON inventory_movements(property_id, movement_date);
CREATE INDEX idx_purchases_property_date ON purchases(property_id, purchase_date);
CREATE INDEX idx_sales_property_date ON sales(property_id, sale_date);
CREATE INDEX idx_production_property_date ON production_records(property_id, production_date);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
