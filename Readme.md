# IPOS-CA — Pharmacy Client Application

## Overview
IPOS-CA is the client-side subsystem of the InfoPharma Ordering System (IPOS). It runs as a desktop application and handles all pharmacy-side operations including stock management, sales, customer accounts, restock orders, and reporting.

---

## How to Run

1. Open the project in **IntelliJ IDEA**
2. Right-click `pom.xml` → **Maven → Reload Project**
3. Make sure the `sql/` folder contains `schema.sql` at the project root
4. Run `app.Main`

The database will be created automatically on first launch at `database/ipos.db`.

---

## Default Login Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Full access |
| `manager` | `manager123` | Reports, reminders, customers, orders |
| `pharmacist` | `pharma123` | Stock and sales only |

---

## Project Structure

```
IPOS-CA/
├── sql/
│   └── schema.sql
├── database/
│   └── ipos.db              (auto-generated on first run)
├── src/
│   ├── main/java/
│   │   ├── app/             Main.java, Session.java
│   │   ├── database/        DAO classes + DatabaseManager
│   │   ├── domain/          User, Product, Customer, DiscountPlan, RestockOrder, CatalogueItem
│   │   ├── integration/     StockServiceImpl, InventoryServiceImpl, OrderStatusImpl
│   │   └── ui/              All panels and screens
│   └── test/java/
│       └── integration/     JUnit 5 tests for subsystem interfaces
└── pom.xml
```

---

## Features

- **Authentication** — real database login with BCrypt password hashing, role-based access
- **Stock Management** — add, edit, remove products, restock, automatic low stock alerts
- **Sales** — account holder sales with invoice generation, occasional customer sales with receipt generation, stock auto-deducted
- **Customer Accounts** — CRUD, credit limits, Fixed and Flexible discount plans, status management (Active / Suspended / In Default)
- **Restock Orders** — place orders, track status (Accepted → Processed → Dispatched → Delivered)
- **Payment Reminders** — 1st and 2nd reminder logic as per specification
- **Reports** — sales turnover, stock availability, customer debt tracking
- **Subsystem Interfaces** — IStockService, IInventoryService, IOrderStatus implemented for integration with IPOS-SA and IPOS-PU

---

## Running the Tests

```
mvn test
```

Or in IntelliJ: right-click `src/test/java` → **Run All Tests**