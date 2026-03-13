-- ---------------------------------------------------------
-- 1. USERS
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('PHARMACIST','ADMIN','MANAGER')),
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------
-- 2. DISCOUNT PLANS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount_plans (
                                              discount_plan_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                              plan_name TEXT NOT NULL UNIQUE,
                                              plan_type TEXT NOT NULL CHECK (plan_type IN ('FIXED', 'FLEXIBLE')),
    discount_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 CHECK (discount_percent >= 0),
    notes TEXT
    );

-- ---------------------------------------------------------
-- 3. CUSTOMER ACCOUNTS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_accounts (
                                                 customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                 account_number TEXT NOT NULL UNIQUE,
                                                 full_name TEXT NOT NULL,
                                                 email TEXT,
                                                 phone TEXT,
                                                 address TEXT,
                                                 credit_limit DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (credit_limit >= 0),
    current_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (current_balance >= 0),
    account_status TEXT NOT NULL DEFAULT 'ACTIVE'
    CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'IN_DEFAULT')),
    discount_plan_id INTEGER,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (discount_plan_id) REFERENCES discount_plans(discount_plan_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 4. PRODUCTS / LOCAL CATALOGUE
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
                                        product_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        item_id TEXT NOT NULL UNIQUE,
                                        description TEXT NOT NULL,
                                        package_type TEXT,
                                        units_in_pack INTEGER NOT NULL DEFAULT 1 CHECK (units_in_pack > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 CHECK (vat_rate >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    min_stock_level INTEGER NOT NULL DEFAULT 0 CHECK (min_stock_level >= 0),
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
    );

-- ---------------------------------------------------------
-- 5. SALES
-- sale_type:
--   ACCOUNT     = sale to account holder
--   OCCASIONAL  = sale to walk-in / non-account customer
-- payment_method:
--   CASH / CARD / CREDIT_ACCOUNT
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
                                     sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     customer_id INTEGER,
                                     processed_by_user_id INTEGER NOT NULL,
                                     sale_type TEXT NOT NULL CHECK (sale_type IN ('ACCOUNT', 'OCCASIONAL')),
    payment_method TEXT CHECK (payment_method IN ('CASH', 'CARD', 'CREDIT_ACCOUNT')),
    subtotal DECIMAL(10,2) NOT NULL CHECK (subtotal >= 0),
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    vat_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (vat_amount >= 0),
    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    sale_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
    FOREIGN KEY (processed_by_user_id) REFERENCES users(user_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 6. SALE ITEMS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sale_items (
                                          sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                          sale_id INTEGER NOT NULL,
                                          product_id INTEGER NOT NULL,
                                          quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    vat_rate DECIMAL(5,2) NOT NULL CHECK (vat_rate >= 0),
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (discount_amount >= 0),
    line_total DECIMAL(10,2) NOT NULL CHECK (line_total >= 0),
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 7. INVOICES
-- Used for account-holder sales
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
                                        invoice_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        invoice_number TEXT NOT NULL UNIQUE,
                                        customer_id INTEGER NOT NULL,
                                        sale_id INTEGER NOT NULL UNIQUE,
                                        invoice_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        amount_due DECIMAL(10,2) NOT NULL CHECK (amount_due >= 0),
    due_date DATE,
    status TEXT NOT NULL DEFAULT 'UNPAID'
    CHECK (status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE')),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 8. RECEIPTS
-- Used for occasional customer sales
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS receipts (
                                        receipt_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        receipt_number TEXT NOT NULL UNIQUE,
                                        sale_id INTEGER NOT NULL UNIQUE,
                                        issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 9. ACCOUNT PAYMENTS
-- Payments made against customer debt / invoices
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS account_payments (
                                                payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                customer_id INTEGER NOT NULL,
                                                invoice_id INTEGER,
                                                recorded_by_user_id INTEGER NOT NULL,
                                                payment_method TEXT NOT NULL CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'CASH')),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
    FOREIGN KEY (recorded_by_user_id) REFERENCES users(user_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 10. RESTOCK ORDERS
-- Orders placed with InfoPharma / IPOS-SA
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS restock_orders (
                                              restock_order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                              order_number TEXT NOT NULL UNIQUE,
                                              merchant_id TEXT,
                                              status TEXT NOT NULL DEFAULT 'ACCEPTED'
                                              CHECK (status IN ('ACCEPTED', 'PROCESSED', 'DISPATCHED', 'DELIVERED')),
    total_value DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (total_value >= 0),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ---------------------------------------------------------
-- 11. RESTOCK ORDER ITEMS
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS restock_order_items (
                                                   restock_order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                   restock_order_id INTEGER NOT NULL,
                                                   product_id INTEGER NOT NULL,
                                                   quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_cost DECIMAL(10,2) NOT NULL CHECK (unit_cost >= 0),
    line_total DECIMAL(10,2) NOT NULL CHECK (line_total >= 0),
    FOREIGN KEY (restock_order_id) REFERENCES restock_orders(restock_order_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 12. PAYMENT REMINDERS
-- Reminder workflow for overdue invoices
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_reminders (
                                                 reminder_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                 customer_id INTEGER NOT NULL,
                                                 invoice_id INTEGER NOT NULL,
                                                 reminder_type TEXT NOT NULL CHECK (reminder_type IN ('FIRST', 'SECOND')),
    reminder_status TEXT NOT NULL DEFAULT 'PENDING'
    CHECK (reminder_status IN ('PENDING', 'SENT', 'NO_NEED')),
    sent_at DATETIME,
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 13. MONTHLY STATEMENTS
-- Optional but useful for manager functionality
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS monthly_statements (
                                                  statement_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                  statement_number TEXT NOT NULL UNIQUE,
                                                  customer_id INTEGER NOT NULL,
                                                  period_start DATE NOT NULL,
                                                  period_end DATE NOT NULL,
                                                  generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  total_due DECIMAL(10,2) NOT NULL CHECK (total_due >= 0),
    status TEXT NOT NULL DEFAULT 'GENERATED'
    CHECK (status IN ('GENERATED', 'SENT', 'PAID')),
    FOREIGN KEY (customer_id) REFERENCES customer_accounts(customer_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 14. ORDER STATUS HISTORY
-- Useful for tracking changes in restock order lifecycle
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
                                                    status_history_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                    restock_order_id INTEGER NOT NULL,
                                                    old_status TEXT,
                                                    new_status TEXT NOT NULL CHECK (new_status IN ('ACCEPTED', 'PROCESSED', 'DISPATCHED', 'DELIVERED')),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (restock_order_id) REFERENCES restock_orders(restock_order_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
    );

-- ---------------------------------------------------------
-- 15. TRANSACTION HISTORY
-- Useful for KAN-34 / history lookup
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction_history (
                                                   transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                   sale_id INTEGER,
                                                   invoice_id INTEGER,
                                                   receipt_id INTEGER,
                                                   transaction_type TEXT NOT NULL
                                                   CHECK (transaction_type IN ('SALE', 'INVOICE', 'RECEIPT', 'ACCOUNT_PAYMENT', 'RESTOCK_ORDER')),
    transaction_datetime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
    FOREIGN KEY (receipt_id) REFERENCES receipts(receipt_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
    );


