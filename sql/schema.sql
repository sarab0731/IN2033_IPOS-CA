-- ---------------------------------------------------------
-- 1. USERS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                                     user_id INT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CHECK (user_role IN ('PHARMACIST','ADMIN','MANAGER'))
    ) ;

-- ---------------------------------------------------------
-- 2. DISCOUNT PLANS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount_plans (
                                              discount_plan_id INT AUTO_INCREMENT PRIMARY KEY,
                                              plan_name VARCHAR(255) NOT NULL UNIQUE,
    plan_type VARCHAR(20) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    CHECK (plan_type IN ('FIXED', 'FLEXIBLE')),
    CHECK (discount_percent >= 0)
    ) ;

-- ---------------------------------------------------------
-- 3. CUSTOMER ACCOUNTS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_accounts (
                                                 customer_id INT AUTO_INCREMENT PRIMARY KEY,
                                                 account_number VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    credit_limit DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    current_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    discount_plan_id INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (credit_limit >= 0),
    CHECK (current_balance >= 0),
    CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'IN_DEFAULT')),
    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(discount_plan_id)
    ON DELETE SET NULL ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 4. PRODUCTS / LOCAL CATALOGUE
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
                                        product_id INT AUTO_INCREMENT PRIMARY KEY,
                                        item_id VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    package_type VARCHAR(100),
    units_in_pack INT NOT NULL DEFAULT 1,
    price DECIMAL(10,2) NOT NULL,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (units_in_pack > 0),
    CHECK (price >= 0),
    CHECK (vat_rate >= 0),
    CHECK (stock_quantity >= 0),
    CHECK (min_stock_level >= 0)
    ) ;

-- ---------------------------------------------------------
-- 5. SALES

-- sale_type:
--   ACCOUNT = sale to account holder
--   OCCASIONAL = sale to walk-in / non-account customer

-- payment_method:
--   CASH / CARD / CREDIT_ACCOUNT

-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
                                     sale_id INT AUTO_INCREMENT PRIMARY KEY,
                                     customer_id INT,
                                     processed_by_user_id INT NOT NULL,
                                     sale_type VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20),
    subtotal DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    vat_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    sale_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (sale_type IN ('ACCOUNT', 'OCCASIONAL')),
    CHECK (payment_method IN ('CASH', 'CARD', 'CREDIT_ACCOUNT')),
    CHECK (subtotal >= 0),
    CHECK (discount_amount >= 0),
    CHECK (vat_amount >= 0),
    CHECK (total_amount >= 0),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (processed_by_user_id) REFERENCES users(user_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 6. SALE ITEMS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sale_items (
                                          sale_item_id INT AUTO_INCREMENT PRIMARY KEY,
                                          sale_id INT NOT NULL,
                                          product_id INT NOT NULL,
                                          quantity INT NOT NULL,
                                          unit_price DECIMAL(10,2) NOT NULL,
    vat_rate DECIMAL(5,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(10,2) NOT NULL,
    CHECK (quantity > 0),
    CHECK (unit_price >= 0),
    CHECK (vat_rate >= 0),
    CHECK (discount_amount >= 0),
    CHECK (line_total >= 0),
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 7. INVOICES
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
                                        invoice_id INT AUTO_INCREMENT PRIMARY KEY,
                                        invoice_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    sale_id INT NOT NULL UNIQUE,
    invoice_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount_due DECIMAL(10,2) NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    CHECK (amount_due >= 0),
    CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 8. RECEIPTS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS receipts (
                                        receipt_id INT AUTO_INCREMENT PRIMARY KEY,
                                        receipt_number VARCHAR(100) NOT NULL UNIQUE,
    sale_id INT NOT NULL UNIQUE,
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 9. ACCOUNT PAYMENTS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS account_payments (
                                                payment_id INT AUTO_INCREMENT PRIMARY KEY,
                                                customer_id INT NOT NULL,
                                                invoice_id INT,
                                                recorded_by_user_id INT NOT NULL,
                                                payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'CASH')),
    CHECK (amount > 0),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (recorded_by_user_id) REFERENCES users(user_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 10. RESTOCK ORDERS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS restock_orders (
                                              restock_order_id INT AUTO_INCREMENT PRIMARY KEY,
                                              order_number VARCHAR(100) NOT NULL UNIQUE,
    merchant_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    total_value DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (status IN ('ACCEPTED', 'PROCESSED', 'DISPATCHED', 'DELIVERED')),
    CHECK (total_value >= 0)
    ) ;

-- ---------------------------------------------------------
-- 11. RESTOCK ORDER ITEMS
-- Orders placed with InfoPharma / IPOS-SA
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS restock_order_items (
                                                   restock_order_item_id INT AUTO_INCREMENT PRIMARY KEY,
                                                   restock_order_id INT NOT NULL,
                                                   product_id INT NOT NULL,
                                                   quantity INT NOT NULL,
                                                   unit_cost DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    CHECK (quantity > 0),
    CHECK (unit_cost >= 0),
    CHECK (line_total >= 0),
    FOREIGN KEY (restock_order_id) REFERENCES restock_orders(restock_order_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 12. PAYMENT REMINDERS
-- Reminder workflow for overdue invoices
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_reminders (
                                                 reminder_id INT AUTO_INCREMENT PRIMARY KEY,
                                                 customer_id INT NOT NULL,
                                                 invoice_id INT NOT NULL,
                                                 reminder_type VARCHAR(20) NOT NULL,
    reminder_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at DATETIME,
    CHECK (reminder_type IN ('FIRST', 'SECOND')),
    CHECK (reminder_status IN ('PENDING', 'SENT', 'NO_NEED')),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 13. MONTHLY STATEMENTS
-- Optional but useful for manager functionality
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS monthly_statements (
                                                  statement_id INT AUTO_INCREMENT PRIMARY KEY,
                                                  statement_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_due DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    CHECK (total_due >= 0),
    CHECK (status IN ('GENERATED', 'SENT', 'PAID')),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 14. ORDER STATUS HISTORY
-- Useful for tracking changes in restock order lifecycle
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
                                                    status_history_id INT AUTO_INCREMENT PRIMARY KEY,
                                                    restock_order_id INT NOT NULL,
                                                    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (new_status IN ('ACCEPTED', 'PROCESSED', 'DISPATCHED', 'DELIVERED')),
    FOREIGN KEY (restock_order_id) REFERENCES restock_orders(restock_order_id)
    ON DELETE CASCADE ON UPDATE CASCADE
    ) ;

-- ---------------------------------------------------------
-- 15. TRANSACTION HISTORY
-- Useful for KAN-34 / history lookup
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction_history (
                                                   transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                                                   sale_id INT,
                                                   invoice_id INT,
                                                   receipt_id INT,
                                                   transaction_type VARCHAR(30) NOT NULL,
    transaction_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    CHECK (transaction_type IN ('SALE', 'INVOICE', 'RECEIPT', 'ACCOUNT_PAYMENT', 'RESTOCK_ORDER')),
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
    ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (receipt_id) REFERENCES receipts(receipt_id)
    ON DELETE SET NULL ON UPDATE CASCADE
    );

