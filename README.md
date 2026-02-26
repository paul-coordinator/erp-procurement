# ERP Procurement System — Java Spring Boot Backend

A production-grade ERP procurement backend that powers the Purchase Order module with real
authentication, role-based workflow, audit trails, and strict business rule enforcement.

---

## 🏗 System Architecture

```
React Frontend (JSX)
        │
        ▼  HTTP / REST + JWT
┌────────────────────────────────────────────────────┐
│           Spring Boot API (port 8080)              │
│                                                    │
│  AuthController    → /api/auth/**                  │
│  PurchaseOrderCtrl → /api/purchase-orders/**       │
│  SupplierCtrl      → /api/suppliers/**             │
│                                                    │
│  Spring Security   → JWT filter on every request  │
│  @PreAuthorize     → Role enforcement per endpoint │
│                                                    │
│  PurchaseOrderService  → All workflow & rules      │
│  AuditService          → All action logging        │
└────────────────────────────────────────────────────┘
        │
        ▼  Spring Data JPA / Hibernate
┌────────────────────────────────────────────────────┐
│  PostgreSQL (prod)  /  H2 in-memory (dev)          │
│                                                    │
│  users              suppliers                      │
│  purchase_orders    purchase_order_items           │
│  purchase_receipts  purchase_invoices              │
│  audit_logs                                        │
└────────────────────────────────────────────────────┘
```

---

## 🗄 Database Schema

| Table                 | Key Columns                                                            |
|-----------------------|------------------------------------------------------------------------|
| `users`               | id, username, password (bcrypt), full_name, email, role, enabled       |
| `suppliers`           | id, supplier_code, supplier_name, contact_person, email, payment_terms |
| `purchase_orders`     | id, po_number, supplier_id, status, grand_total, total_received, total_billed, version (OL) |
| `purchase_order_items`| id, po_id, item_description, ordered_qty, received_qty, unit_price, line_total |
| `purchase_receipts`   | id, po_id, receipt_number, receipt_date, received_amount, received_by  |
| `purchase_invoices`   | id, po_id, invoice_number, invoice_date, invoice_amount, payment_status |
| `audit_logs`          | id, action, entity_type, entity_id, performed_by, previous_status, new_status |

---

## 🔐 Roles & Permissions

| Action                    | Coordinator | Manager | Finance | Admin |
|---------------------------|:-----------:|:-------:|:-------:|:-----:|
| Create PO (Draft)         | ✅          | ✅      |         | ✅    |
| Submit PO for Approval    | ✅          | ✅      |         | ✅    |
| Approve / Reject PO       |             | ✅      |         | ✅    |
| Cancel PO                 |             | ✅      |         | ✅    |
| Record Goods Receipt      | ✅          | ✅      |         | ✅    |
| Post Invoice              |             |         | ✅      | ✅    |
| View All POs & KPIs       | ✅          | ✅      | ✅      | ✅    |
| Manage Suppliers          |             | ✅      |         | ✅    |

---

## 📋 Procurement Workflow (Enforced in Backend)

```
DRAFT ──► SUBMITTED ──► APPROVED ──► TO_RECEIVE ──► TO_BILL ──► COMPLETED
  │                        │                                         ▲
  │                        └──► DRAFT (rejected, back for edit)      │
  └──────────────────────────── CANCELLED ◄────────────────────────(any non-completed)
```

**Completion rule (MANDATORY):** A PO can only reach COMPLETED status when:
- `total_received >= grand_total` (fully received)
- `total_billed >= grand_total` (fully billed)

Both checks are enforced server-side. The frontend cannot override this.

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- Java 17+
- Maven 3.8+
- (Optional) PostgreSQL 14+ for production

### Run with H2 (zero setup)
```bash
cd erp-system
mvn spring-boot:run
```

The app starts at: `http://localhost:8080`
H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:erpdb`
- Username: `sa` | Password: _(blank)_

### Run with PostgreSQL
1. Create database:
```sql
CREATE DATABASE erp_procurement;
CREATE USER erp_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE erp_procurement TO erp_user;
```
2. Update `application.properties` (uncomment PostgreSQL block, comment H2 block)
3. Change `spring.jpa.hibernate.ddl-auto=create-drop` to `update` or `validate`

---

## 🔑 Default Login Credentials

| Username      | Password    | Role                        |
|---------------|-------------|-----------------------------|
| `admin`       | `Admin@123` | ADMIN (all permissions)     |
| `coordinator` | `Coord@123` | PROCUREMENT_COORDINATOR     |
| `manager`     | `Mgr@123`   | PURCHASING_MANAGER          |
| `finance`     | `Fin@123`   | FINANCE                     |

---

## 📡 API Reference

### Authentication
```
POST /api/auth/login
Body: { "username": "coordinator", "password": "Coord@123" }
Response: { "token": "eyJ...", "role": "ROLE_PROCUREMENT_COORDINATOR", ... }

GET  /api/auth/me          → Current user info (requires Bearer token)
POST /api/auth/logout      → Logout (audit logged)
```

### Purchase Orders
```
GET    /api/purchase-orders                     → List all (paginated)
GET    /api/purchase-orders?status=TO_RECEIVE   → Filter by status
GET    /api/purchase-orders/{id}                → Get single PO
POST   /api/purchase-orders                     → Create PO (COORDINATOR)
POST   /api/purchase-orders/{id}/submit         → Submit for approval (COORDINATOR)
POST   /api/purchase-orders/{id}/approve        → Approve (MANAGER)
POST   /api/purchase-orders/{id}/reject         → Reject with reason (MANAGER)
POST   /api/purchase-orders/{id}/receive        → Record goods receipt
POST   /api/purchase-orders/{id}/invoice        → Post invoice (FINANCE)
POST   /api/purchase-orders/{id}/cancel         → Cancel PO (MANAGER)
GET    /api/purchase-orders/kpi/dashboard       → KPI summary
GET    /api/purchase-orders/{id}/audit          → Full audit trail for PO
```

### Suppliers
```
GET    /api/suppliers        → List all suppliers
GET    /api/suppliers/{id}   → Get supplier
POST   /api/suppliers        → Create supplier (MANAGER/ADMIN)
PUT    /api/suppliers/{id}   → Update supplier (MANAGER/ADMIN)
```

---

## 📬 Example API Calls (curl)

### 1. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"coordinator","password":"Coord@123"}'
```

### 2. Create a Purchase Order
```bash
curl -X POST http://localhost:8080/api/purchase-orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "supplierId": 1,
    "orderDate": "2026-02-26",
    "expectedDeliveryDate": "2026-03-05",
    "remarks": "Urgent HVAC parts for site",
    "items": [
      {
        "itemDescription": "3-Ton Compressor Unit",
        "itemCode": "HVAC-001",
        "unit": "PCS",
        "orderedQty": 2,
        "unitPrice": 1250.00
      }
    ]
  }'
```

### 3. Submit for Approval
```bash
curl -X POST http://localhost:8080/api/purchase-orders/1/submit \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Approve (login as manager first)
```bash
curl -X POST http://localhost:8080/api/purchase-orders/1/approve \
  -H "Authorization: Bearer MANAGER_TOKEN"
```

### 5. Record Goods Receipt
```bash
curl -X POST http://localhost:8080/api/purchase-orders/1/receive \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiptDate":"2026-03-05","receivedAmount":2500.00,"notes":"All items in good condition"}'
```

### 6. Post Invoice (login as finance)
```bash
curl -X POST http://localhost:8080/api/purchase-orders/1/invoice \
  -H "Authorization: Bearer FINANCE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"invoiceNumber":"INV-2026-001","invoiceDate":"2026-03-06","invoiceAmount":2500.00}'
```

### 7. View KPI Dashboard
```bash
curl http://localhost:8080/api/purchase-orders/kpi/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🔒 Concurrency & Safety

- **Optimistic Locking:** The `version` field on `PurchaseOrder` prevents two users from
  saving conflicting changes simultaneously. The second save will receive a 409 Conflict error.
- **Transactional integrity:** All state changes use `@Transactional` — either fully commit or
  fully roll back.
- **Async audit logging:** Audit logs are written asynchronously (`@Async`) to avoid slowing down
  the main transaction.

---

## 🏢 How This Maps to Real ERP Platforms

| This System              | SAP S/4HANA           | ERPNext (Frappe)         |
|--------------------------|-----------------------|--------------------------|
| PurchaseOrder entity     | ME21N / Purchase Order| Purchase Order DocType   |
| PurchaseReceipt          | MIGO / GR             | Purchase Receipt         |
| PurchaseInvoice          | MIRO / Invoice Verify | Purchase Invoice         |
| Workflow transitions     | Release Strategy      | Workflow State Machine   |
| AuditLog                 | Change Document (CDHDR)| Version / Comment Trail  |
| Optimistic locking       | Lock object           | Document locking         |
| Role-based access        | Authorization Objects | Role & Permission Manager|

---

## 📁 Project Structure

```
erp-system/
├── pom.xml
└── src/main/
    ├── java/com/erp/procurement/
    │   ├── ProcurementApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java      ← JWT + Role security
    │   │   └── DataSeeder.java          ← Default users & suppliers
    │   ├── controller/
    │   │   ├── AuthController.java      ← Login, logout, /me
    │   │   ├── PurchaseOrderController.java
    │   │   └── SupplierController.java
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Supplier.java
    │   │   ├── PurchaseOrder.java       ← @Version optimistic lock
    │   │   ├── PurchaseOrderItem.java
    │   │   ├── PurchaseReceipt.java
    │   │   ├── PurchaseInvoice.java
    │   │   └── AuditLog.java
    │   ├── enums/
    │   │   ├── PurchaseOrderStatus.java
    │   │   ├── Role.java
    │   │   └── AuditAction.java
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java
    │   ├── repository/                  ← Spring Data JPA interfaces
    │   ├── security/
    │   │   └── JwtUtil.java             ← JWT generate/validate
    │   └── service/
    │       ├── PurchaseOrderService.java ← All ERP business logic
    │       └── AuditService.java
    └── resources/
        └── application.properties
```
