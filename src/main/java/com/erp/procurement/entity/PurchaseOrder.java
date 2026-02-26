package com.erp.procurement.entity;

import com.erp.procurement.enums.PurchaseOrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Column
    private LocalDate expectedDeliveryDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grandTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalReceived = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBilled = BigDecimal.ZERO;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime approvedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseReceipt> receipts = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseInvoice> invoices = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.totalReceived == null) this.totalReceived = BigDecimal.ZERO;
        if (this.totalBilled == null)   this.totalBilled   = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public PurchaseOrder() {}

    // Business helpers
    public boolean isFullyReceived() {
        return totalReceived != null && grandTotal != null
                && totalReceived.compareTo(grandTotal) >= 0;
    }

    public boolean isFullyBilled() {
        return totalBilled != null && grandTotal != null
                && totalBilled.compareTo(grandTotal) >= 0;
    }

    public boolean isEditable() {
        return status == PurchaseOrderStatus.DRAFT;
    }

    public int getBilledPercent() {
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) return 0;
        return totalBilled.multiply(BigDecimal.valueOf(100))
                .divide(grandTotal, 0, RoundingMode.HALF_UP).intValue();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public PurchaseOrderStatus getStatus() { return status; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public BigDecimal getTotalReceived() { return totalReceived != null ? totalReceived : BigDecimal.ZERO; }
    public void setTotalReceived(BigDecimal totalReceived) { this.totalReceived = totalReceived; }

    public BigDecimal getTotalBilled() { return totalBilled != null ? totalBilled : BigDecimal.ZERO; }
    public void setTotalBilled(BigDecimal totalBilled) { this.totalBilled = totalBilled; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }

    public List<PurchaseReceipt> getReceipts() { return receipts; }
    public void setReceipts(List<PurchaseReceipt> receipts) { this.receipts = receipts; }

    public List<PurchaseInvoice> getInvoices() { return invoices; }
    public void setInvoices(List<PurchaseInvoice> invoices) { this.invoices = invoices; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PurchaseOrder po = new PurchaseOrder();
        public Builder poNumber(String v)              { po.poNumber = v; return this; }
        public Builder supplier(Supplier v)            { po.supplier = v; return this; }
        public Builder createdBy(User v)               { po.createdBy = v; return this; }
        public Builder approvedBy(User v)              { po.approvedBy = v; return this; }
        public Builder status(PurchaseOrderStatus v)   { po.status = v; return this; }
        public Builder orderDate(LocalDate v)          { po.orderDate = v; return this; }
        public Builder expectedDeliveryDate(LocalDate v){ po.expectedDeliveryDate = v; return this; }
        public Builder grandTotal(BigDecimal v)        { po.grandTotal = v; return this; }
        public Builder totalReceived(BigDecimal v)     { po.totalReceived = v; return this; }
        public Builder totalBilled(BigDecimal v)       { po.totalBilled = v; return this; }
        public Builder remarks(String v)               { po.remarks = v; return this; }
        public PurchaseOrder build()                   { return po; }
    }
}
