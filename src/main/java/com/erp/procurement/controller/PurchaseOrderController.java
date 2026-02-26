package com.erp.procurement.controller;

import com.erp.procurement.entity.*;
import com.erp.procurement.enums.PurchaseOrderStatus;
import com.erp.procurement.service.AuditService;
import com.erp.procurement.service.PurchaseOrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;
    private final AuditService auditService;

    public PurchaseOrderController(PurchaseOrderService poService, AuditService auditService) {
        this.poService = poService;
        this.auditService = auditService;
    }

    // ── GET ALL ────────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String status) {

        if (status != null && !status.isBlank()) {
            PurchaseOrderStatus poStatus = PurchaseOrderStatus.valueOf(status.toUpperCase());
            Page<PurchaseOrder> result = poService.getOrdersByStatus(poStatus, page, size);
            return ResponseEntity.ok(toPageResponse(result));
        }
        Page<PurchaseOrder> result = poService.getAllOrders(page, size, sort);
        return ResponseEntity.ok(toPageResponse(result));
    }

    // ── GET ONE ────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(poService.getOrderById(id)));
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body, Authentication auth) {
        Long supplierId = Long.parseLong(body.get("supplierId").toString());
        LocalDate orderDate = body.containsKey("orderDate")
                ? LocalDate.parse(body.get("orderDate").toString()) : null;
        LocalDate expectedDelivery = body.containsKey("expectedDeliveryDate")
                ? LocalDate.parse(body.get("expectedDeliveryDate").toString()) : null;
        String remarks = body.getOrDefault("remarks", "").toString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

        PurchaseOrder created = poService.createPurchaseOrder(
                supplierId, orderDate, expectedDelivery, remarks, items, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    // ── SUBMIT FOR APPROVAL ────────────────────────────────────────────────────
    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(toResponse(poService.submitForApproval(id, auth.getName())));
    }

    // ── APPROVE ────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(toResponse(poService.approvePurchaseOrder(id, auth.getName())));
    }

    // ── REJECT ─────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     Authentication auth) {
        return ResponseEntity.ok(toResponse(
                poService.rejectPurchaseOrder(id, body.getOrDefault("reason", "No reason"), auth.getName())));
    }

    // ── RECEIVE GOODS ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/receive")
    public ResponseEntity<?> receive(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      Authentication auth) {
        LocalDate receiptDate = body.containsKey("receiptDate")
                ? LocalDate.parse(body.get("receiptDate").toString()) : null;
        BigDecimal amount = new BigDecimal(body.get("receivedAmount").toString());
        String notes = body.getOrDefault("notes", "").toString();
        PurchaseReceipt receipt = poService.receiveGoods(id, receiptDate, amount, notes, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toReceiptResponse(receipt));
    }

    // ── POST INVOICE ───────────────────────────────────────────────────────────
    @PostMapping("/{id}/invoice")
    public ResponseEntity<?> postInvoice(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        String invoiceNumber = body.get("invoiceNumber").toString();
        LocalDate invoiceDate = LocalDate.parse(body.get("invoiceDate").toString());
        LocalDate dueDate = body.containsKey("dueDate")
                ? LocalDate.parse(body.get("dueDate").toString()) : null;
        BigDecimal amount = new BigDecimal(body.get("invoiceAmount").toString());
        String notes = body.getOrDefault("notes", "").toString();
        PurchaseInvoice invoice = poService.postInvoice(
                id, invoiceNumber, invoiceDate, dueDate, amount, notes, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toInvoiceResponse(invoice));
    }

    // ── CANCEL ─────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     Authentication auth) {
        return ResponseEntity.ok(toResponse(
                poService.cancelPurchaseOrder(id, body.getOrDefault("reason", "No reason"), auth.getName())));
    }

    // ── KPI DASHBOARD ──────────────────────────────────────────────────────────
    @GetMapping("/kpi/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(poService.getKpiDashboard());
    }

    // ── AUDIT LOG FOR PO ───────────────────────────────────────────────────────
    @GetMapping("/{id}/audit")
    public ResponseEntity<?> auditLog(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getLogsForEntity("PurchaseOrder", id));
    }

    // ── RESPONSE MAPPERS ───────────────────────────────────────────────────────
    private Map<String, Object> toResponse(PurchaseOrder po) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", po.getId());
        m.put("poNumber", po.getPoNumber());
        m.put("supplierId", po.getSupplier().getId());
        m.put("supplierName", po.getSupplier().getSupplierName());
        m.put("supplierCode", po.getSupplier().getSupplierCode());
        m.put("status", po.getStatus());
        m.put("orderDate", po.getOrderDate());
        m.put("expectedDeliveryDate", po.getExpectedDeliveryDate());
        m.put("grandTotal", po.getGrandTotal());
        m.put("totalReceived", po.getTotalReceived());
        m.put("totalBilled", po.getTotalBilled());
        m.put("billedPercent", po.getBilledPercent());
        m.put("createdBy", po.getCreatedBy().getFullName());
        m.put("approvedBy", po.getApprovedBy() != null ? po.getApprovedBy().getFullName() : null);
        m.put("approvedAt", po.getApprovedAt());
        m.put("remarks", po.getRemarks());
        m.put("createdAt", po.getCreatedAt());
        m.put("updatedAt", po.getUpdatedAt());
        m.put("version", po.getVersion());
        if (po.getItems() != null) {
            m.put("items", po.getItems().stream().map(i -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", i.getId());
                item.put("itemDescription", i.getItemDescription());
                item.put("itemCode", i.getItemCode());
                item.put("unit", i.getUnit());
                item.put("orderedQty", i.getOrderedQty());
                item.put("receivedQty", i.getReceivedQty());
                item.put("unitPrice", i.getUnitPrice());
                item.put("lineTotal", i.getLineTotal());
                return item;
            }).collect(Collectors.toList()));
        }
        return m;
    }

    private Map<String, Object> toPageResponse(Page<PurchaseOrder> page) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", page.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("currentPage", page.getNumber());
        result.put("pageSize", page.getSize());
        return result;
    }

    private Map<String, Object> toReceiptResponse(PurchaseReceipt r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("receiptNumber", r.getReceiptNumber());
        m.put("receiptDate", r.getReceiptDate());
        m.put("receivedAmount", r.getReceivedAmount());
        m.put("receivedBy", r.getReceivedBy().getFullName());
        m.put("notes", r.getNotes() != null ? r.getNotes() : "");
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    private Map<String, Object> toInvoiceResponse(PurchaseInvoice inv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", inv.getId());
        m.put("invoiceNumber", inv.getInvoiceNumber());
        m.put("invoiceDate", inv.getInvoiceDate());
        m.put("invoiceAmount", inv.getInvoiceAmount());
        m.put("paymentStatus", inv.getPaymentStatus());
        m.put("postedBy", inv.getPostedBy().getFullName());
        m.put("createdAt", inv.getCreatedAt());
        return m;
    }
}
