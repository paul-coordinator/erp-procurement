package com.erp.procurement.dto;

import com.erp.procurement.enums.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Container class for all DTO definitions.
 * These are used for documentation/reference purposes.
 * The controllers use Map<String, Object> directly for flexibility.
 */
public class Dtos {

    // ─── Auth ─────────────────────────────────────────────
    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;
        private String username;
        private String fullName;
        private String role;
        private long expiresIn;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    }

    // ─── Purchase Order ───────────────────────────────────
    public static class CreatePoRequest {
        private Long supplierId;
        private LocalDate orderDate;
        private LocalDate expectedDeliveryDate;
        private String remarks;
        private List<ItemRequest> items;
        public Long getSupplierId() { return supplierId; }
        public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
        public LocalDate getOrderDate() { return orderDate; }
        public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
        public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
        public void setExpectedDeliveryDate(LocalDate v) { this.expectedDeliveryDate = v; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public List<ItemRequest> getItems() { return items; }
        public void setItems(List<ItemRequest> items) { this.items = items; }
    }

    public static class ItemRequest {
        private String itemDescription;
        private String itemCode;
        private String unit;
        private BigDecimal orderedQty;
        private BigDecimal unitPrice;
        public String getItemDescription() { return itemDescription; }
        public void setItemDescription(String v) { this.itemDescription = v; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public BigDecimal getOrderedQty() { return orderedQty; }
        public void setOrderedQty(BigDecimal v) { this.orderedQty = v; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    }

    // ─── KPI Dashboard ────────────────────────────────────
    public static class DashboardResponse {
        private long totalOrders;
        private long draftCount;
        private long submittedCount;
        private long approvedCount;
        private long toReceiveCount;
        private long toBillCount;
        private long completedCount;
        private long cancelledCount;
        private BigDecimal pendingValue;
        private BigDecimal completedValue;
        private BigDecimal totalBilledValue;
        // Getters/setters omitted for brevity — used for documentation only
    }
}
