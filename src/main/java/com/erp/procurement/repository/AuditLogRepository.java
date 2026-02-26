package com.erp.procurement.repository;

import com.erp.procurement.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, Long entityId);
    Page<AuditLog> findByPerformedByOrderByPerformedAtDesc(String username, Pageable pageable);
}
