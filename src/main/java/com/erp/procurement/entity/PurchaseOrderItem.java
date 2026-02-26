package com.erp.procurement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false, length = 255)
    private String itemDescription;

    @Column(length = 50)
    private String itemCode;

    @Column(length = 30)
    private String unit;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal orderedQty;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;

    @PrePersist
    @PreUpdate
    protected void calcLineTotal() {
        if (unitPrice != null && orderedQty != null) {
            this.lineTotal = unitPrice.multiply(orderedQty);
        }
    }

    public PurchaseOrderItem() {}

    public BigDecimal getRemainingQty() {
        return orderedQty.subtract(receivedQty);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal orderedQty) { this.orderedQty = orderedQty; }

    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal receivedQty) { this.receivedQty = receivedQty; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PurchaseOrderItem i = new PurchaseOrderItem();
        public Builder purchaseOrder(PurchaseOrder v)   { i.purchaseOrder = v; return this; }
        public Builder itemDescription(String v)        { i.itemDescription = v; return this; }
        public Builder itemCode(String v)               { i.itemCode = v; return this; }
        public Builder unit(String v)                   { i.unit = v; return this; }
        public Builder orderedQty(BigDecimal v)         { i.orderedQty = v; return this; }
        public Builder receivedQty(BigDecimal v)        { i.receivedQty = v; return this; }
        public Builder unitPrice(BigDecimal v)          { i.unitPrice = v; return this; }
        public Builder lineTotal(BigDecimal v)          { i.lineTotal = v; return this; }
        public PurchaseOrderItem build()                { return i; }
    }
}
