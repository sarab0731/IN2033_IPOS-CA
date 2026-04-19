# IPOS-CA
 
**InfoPharma Ordering System — Client Application**
 
IPOS-CA is a pharmacy point-of-sale desktop application used by Cosymed Ltd, a pharmacy business, to manage their day-to-day operations.
 
Staff can process sales at the counter — accepting cash, card, or account-based payment — and the system automatically handles customer discounts, whether that's a fixed percentage off every purchase or a flexible earn-and-redeem credit scheme based on how much a customer spends each month.
 
The application connects to two external systems: a wholesale supplier (SA) that Cosymed orders stock from, and an online pharmacy portal (PU) where customers can place orders remotely. When stock changes hands in either direction, the systems stay in sync automatically.
 
Other features include full stock management with low-stock alerts, customer account management with payment reminders and automatic account suspension for overdue balances, restock ordering directly through the supplier, and a staff login system with role-based access.
 
 
## Run from command line
 
```bash
docker compose up --build
```
  
## Login Details
 
| Role       | Username     | Password      |
|------------|--------------|---------------|
| Admin      | `admin`      | `admin123`    |
| Manager    | `manager`    | `manager123`  |
| Pharmacist | `pharmacist` | `pharma123`   |
 
 
## User Roles
 
- **Admin** — Full system access including user management
- **Manager** — Reports, customer account management, stock ordering
- **Pharmacist** — Point of sale, stock viewing, day-to-day operations
 
