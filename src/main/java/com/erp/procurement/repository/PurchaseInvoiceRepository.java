package com.erp.procurement.repository;

import com.erp.procurement.entity.PurchaseInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    List<PurchaseInvoice> findByPurchaseOrder_Id(Long poId);
    boolean existsByInvoiceNumber(String invoiceNumber);
}
