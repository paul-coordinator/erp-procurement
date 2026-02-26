package com.erp.procurement.repository;

import com.erp.procurement.entity.PurchaseOrder;
import com.erp.procurement.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    Page<PurchaseOrder> findBySupplier_SupplierNameContainingIgnoreCaseOrPoNumberContainingIgnoreCase(
            String supplierName, String poNumber, Pageable pageable);

    @Query("SELECT COUNT(p) FROM PurchaseOrder p WHERE p.status = :status")
    long countByStatus(@Param("status") PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(p.grandTotal), 0) FROM PurchaseOrder p WHERE p.status = :status")
    BigDecimal sumGrandTotalByStatus(@Param("status") PurchaseOrderStatus status);

    @Query("SELECT COALESCE(SUM(p.totalBilled), 0) FROM PurchaseOrder p WHERE p.status = 'COMPLETED'")
    BigDecimal sumTotalBilled();

    @Query("SELECT p FROM PurchaseOrder p WHERE p.status IN ('TO_RECEIVE','TO_BILL','APPROVED') ORDER BY p.expectedDeliveryDate ASC")
    List<PurchaseOrder> findActivePendingOrders();

    boolean existsByPoNumber(String poNumber);
}
