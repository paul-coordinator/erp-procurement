package com.erp.procurement.service;

import com.erp.procurement.entity.*;
import com.erp.procurement.enums.AuditAction;
import com.erp.procurement.enums.PurchaseOrderStatus;
import com.erp.procurement.enums.Role;
import com.erp.procurement.repository.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Service
public class PurchaseOrderService {

    private static final Logger log = Logger.getLogger(PurchaseOrderService.class.getName());

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final PurchaseReceiptRepository receiptRepository;
    private final PurchaseInvoiceRepository invoiceRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private final AtomicLong poSequence = new AtomicLong(1000);

    public PurchaseOrderService(PurchaseOrderRepository poRepository,
                                 PurchaseOrderItemRepository itemRepository,
                                 PurchaseReceiptRepository receiptRepository,
                                 PurchaseInvoiceRepository invoiceRepository,
                                 SupplierRepository supplierRepository,
                                 UserRepository userRepository,
                                 AuditService auditService) {
        this.poRepository = poRepository;
        this.itemRepository = itemRepository;
        this.receiptRepository = receiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ── CREATE PO ──────────────────────────────────────────────────────────────
    @Transactional
    public PurchaseOrder createPurchaseOrder(Long supplierId, LocalDate orderDate,
                                              LocalDate expectedDelivery, String remarks,
                                              List<Map<String, Object>> itemsData, String username) {
        User user = loadUser(username);
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + supplierId));

        String poNumber = generatePoNumber();

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .supplier(supplier)
                .createdBy(user)
                .status(PurchaseOrderStatus.DRAFT)
                .orderDate(orderDate != null ? orderDate : LocalDate.now())
                .expectedDeliveryDate(expectedDelivery)
                .remarks(remarks)
                .grandTotal(BigDecimal.ZERO)
                .totalReceived(BigDecimal.ZERO)
                .totalBilled(BigDecimal.ZERO)
                .build();

        List<PurchaseOrderItem> items = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map<String, Object> data : itemsData) {
            BigDecimal qty = new BigDecimal(data.get("orderedQty").toString());
            BigDecimal price = new BigDecimal(data.get("unitPrice").toString());
            BigDecimal lineTotal = qty.multiply(price);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .itemDescription(data.get("itemDescription").toString())
                    .itemCode(data.getOrDefault("itemCode", "").toString())
                    .unit(data.getOrDefault("unit", "PCS").toString())
                    .orderedQty(qty)
                    .unitPrice(price)
                    .lineTotal(lineTotal)
                    .receivedQty(BigDecimal.ZERO)
                    .build();
            items.add(item);
            grandTotal = grandTotal.add(lineTotal);
        }

        if (items.isEmpty()) throw new RuntimeException("Purchase order must have at least one item");
        if (grandTotal.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("Grand total must be greater than zero");

        po.setGrandTotal(grandTotal);
        po.setItems(items);

        PurchaseOrder saved = poRepository.save(po);

        auditService.log(AuditAction.PO_CREATED, "PurchaseOrder", saved.getId(),
                username, null, "DRAFT",
                "PO " + poNumber + " created for supplier " + supplier.getSupplierName() + " | Total: " + grandTotal);

        log.info("PO created: " + poNumber + " by " + username + " | Total: " + grandTotal);
        return saved;
    }

    // ── SUBMIT FOR APPROVAL ────────────────────────────────────────────────────
    @Transactional
    public PurchaseOrder submitForApproval(Long poId, String username) {
        PurchaseOrder po = loadPO(poId);
        validateTransition(po, PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.SUBMITTED);

        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        PurchaseOrder saved = poRepository.save(po);

        auditService.log(AuditAction.PO_SUBMITTED, "PurchaseOrder", poId, username,
                "DRAFT", "SUBMITTED", "PO submitted for approval");
        return saved;
    }

    // ── APPROVE PO ────────────────────────────────────────────────────────────
    @Transactional
    public PurchaseOrder approvePurchaseOrder(Long poId, String username) {
        User approver = loadUser(username);
        if (approver.getRole() != Role.ROLE_PURCHASING_MANAGER && approver.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only Purchasing Managers can approve Purchase Orders");
        }

        PurchaseOrder po = loadPO(poId);
        validateTransition(po, PurchaseOrderStatus.SUBMITTED, PurchaseOrderStatus.APPROVED);

        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(approver);
        po.setApprovedAt(java.time.LocalDateTime.now());
        PurchaseOrder saved = poRepository.save(po);

        auditService.log(AuditAction.PO_APPROVED, "PurchaseOrder", poId, username,
                "SUBMITTED", "APPROVED", "PO approved by " + approver.getFullName());
        return saved;
    }

    // ── REJECT PO ─────────────────────────────────────────────────────────────
    @Transactional
    public PurchaseOrder rejectPurchaseOrder(Long poId, String reason, String username) {
        User approver = loadUser(username);
        if (approver.getRole() != Role.ROLE_PURCHASING_MANAGER && approver.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Only Purchasing Managers can reject Purchase Orders");
        }

        PurchaseOrder po = loadPO(poId);
        if (po.getStatus() != PurchaseOrderStatus.SUBMITTED) {
            throw new RuntimeException("Only SUBMITTED POs can be rejected");
        }

        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setRemarks((po.getRemarks() != null ? po.getRemarks() + " | " : "") + "REJECTED: " + reason);
        PurchaseOrder saved = poRepository.save(po);

        auditService.log(AuditAction.PO_REJECTED, "PurchaseOrder", poId, username,
                "SUBMITTED", "DRAFT", "Rejected: " + reason);
        return saved;
    }

    // ── RECEIVE GOODS ──────────────────────────────────────────────────────────
    @Transactional
    public PurchaseReceipt receiveGoods(Long poId, LocalDate receiptDate,
                                         BigDecimal receivedAmount, String notes, String username) {
        PurchaseOrder po = loadPO(poId);
        if (po.getStatus() != PurchaseOrderStatus.APPROVED && po.getStatus() != PurchaseOrderStatus.TO_RECEIVE) {
            throw new RuntimeException("Goods can only be received for APPROVED or TO_RECEIVE orders. Current: " + po.getStatus());
        }
        if (receivedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Received amount must be greater than zero");
        }
        BigDecimal newTotalReceived = po.getTotalReceived().add(receivedAmount);
        if (newTotalReceived.compareTo(po.getGrandTotal()) > 0) {
            throw new RuntimeException("Total received (" + newTotalReceived + ") cannot exceed PO total (" + po.getGrandTotal() + ")");
        }

        User receiver = loadUser(username);
        String receiptNumber = "REC-" + po.getPoNumber() + "-" + System.currentTimeMillis() % 10000;

        PurchaseReceipt receipt = PurchaseReceipt.builder()
                .purchaseOrder(po)
                .receivedBy(receiver)
                .receiptNumber(receiptNumber)
                .receiptDate(receiptDate != null ? receiptDate : LocalDate.now())
                .receivedAmount(receivedAmount)
                .notes(notes)
                .build();

        receiptRepository.save(receipt);

        po.setTotalReceived(newTotalReceived);
        if (po.isFullyReceived() && po.isFullyBilled()) {
            po.setStatus(PurchaseOrderStatus.COMPLETED);
        } else if (po.isFullyReceived()) {
            po.setStatus(PurchaseOrderStatus.TO_BILL);
        } else {
            po.setStatus(PurchaseOrderStatus.TO_RECEIVE);
        }
        poRepository.save(po);

        auditService.log(AuditAction.PO_RECEIVED, "PurchaseOrder", poId, username,
                null, po.getStatus().name(), "Received: " + receivedAmount + " | Total: " + newTotalReceived);
        return receipt;
    }

    // ── POST INVOICE ───────────────────────────────────────────────────────────
    @Transactional
    public PurchaseInvoice postInvoice(Long poId, String invoiceNumber, LocalDate invoiceDate,
                                        LocalDate dueDate, BigDecimal invoiceAmount, String notes, String username) {
        PurchaseOrder po = loadPO(poId);
        if (po.getStatus() != PurchaseOrderStatus.TO_BILL
                && po.getStatus() != PurchaseOrderStatus.TO_RECEIVE
                && po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new RuntimeException("Invoice can only be posted for APPROVED, TO_RECEIVE, or TO_BILL orders");
        }
        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            throw new RuntimeException("Invoice number already exists: " + invoiceNumber);
        }
        if (invoiceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invoice amount must be greater than zero");
        }
        BigDecimal newTotalBilled = po.getTotalBilled().add(invoiceAmount);
        if (newTotalBilled.compareTo(po.getGrandTotal()) > 0) {
            throw new RuntimeException("Total billed (" + newTotalBilled + ") would exceed PO total (" + po.getGrandTotal() + ")");
        }

        User poster = loadUser(username);
        PurchaseInvoice invoice = PurchaseInvoice.builder()
                .purchaseOrder(po)
                .postedBy(poster)
                .invoiceNumber(invoiceNumber)
                .invoiceDate(invoiceDate)
                .dueDate(dueDate)
                .invoiceAmount(invoiceAmount)
                .notes(notes)
                .paymentStatus("UNPAID")
                .build();

        invoiceRepository.save(invoice);

        po.setTotalBilled(newTotalBilled);
        if (po.isFullyReceived() && po.isFullyBilled()) {
            po.setStatus(PurchaseOrderStatus.COMPLETED);
        } else if (po.isFullyBilled()) {
            po.setStatus(PurchaseOrderStatus.TO_RECEIVE);
        }
        poRepository.save(po);

        auditService.log(AuditAction.PO_BILLED, "PurchaseOrder", poId, username,
                null, po.getStatus().name(), "Invoice: " + invoiceNumber + " | Amount: " + invoiceAmount);
        return invoice;
    }

    // ── CANCEL PO ─────────────────────────────────────────────────────────────
    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long poId, String reason, String username) {
        PurchaseOrder po = loadPO(poId);
        if (po.getStatus() == PurchaseOrderStatus.COMPLETED || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel a " + po.getStatus() + " purchase order");
        }
        if (po.getTotalReceived().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot cancel a PO with goods already received");
        }

        String previousStatus = po.getStatus().name();
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po.setRemarks((po.getRemarks() != null ? po.getRemarks() + " | " : "") + "CANCELLED: " + reason);
        PurchaseOrder saved = poRepository.save(po);

        auditService.log(AuditAction.PO_CANCELLED, "PurchaseOrder", poId, username,
                previousStatus, "CANCELLED", "Cancelled: " + reason);
        return saved;
    }

    // ── QUERIES ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<PurchaseOrder> getAllOrders(int page, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.DESC, sort != null ? sort : "createdAt");
        return poRepository.findAll(PageRequest.of(page, size, sorting));
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> getOrdersByStatus(PurchaseOrderStatus status, int page, int size) {
        return poRepository.findByStatus(status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getOrderById(Long id) { return loadPO(id); }

    @Transactional(readOnly = true)
    public Map<String, Object> getKpiDashboard() {
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("totalOrders", poRepository.count());
        for (PurchaseOrderStatus status : PurchaseOrderStatus.values()) {
            kpi.put(status.name().toLowerCase() + "Count", poRepository.countByStatus(status));
        }
        kpi.put("pendingValue",
                poRepository.sumGrandTotalByStatus(PurchaseOrderStatus.TO_RECEIVE)
                        .add(poRepository.sumGrandTotalByStatus(PurchaseOrderStatus.TO_BILL)));
        kpi.put("completedValue",  poRepository.sumGrandTotalByStatus(PurchaseOrderStatus.COMPLETED));
        kpi.put("totalBilledValue", poRepository.sumTotalBilled());
        return kpi;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private PurchaseOrder loadPO(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found: " + id));
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private void validateTransition(PurchaseOrder po, PurchaseOrderStatus expected, PurchaseOrderStatus target) {
        if (po.getStatus() != expected) {
            throw new RuntimeException("Cannot transition to " + target
                    + ". Current: " + po.getStatus() + ", expected: " + expected);
        }
    }

    private String generatePoNumber() {
        String year = DateTimeFormatter.ofPattern("yyyy").format(LocalDate.now());
        long seq = poSequence.incrementAndGet();
        String candidate = "PO-" + year + "-" + String.format("%04d", seq);
        while (poRepository.existsByPoNumber(candidate)) {
            candidate = "PO-" + year + "-" + String.format("%04d", poSequence.incrementAndGet());
        }
        return candidate;
    }
}
