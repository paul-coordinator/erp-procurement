package com.erp.procurement.service;

import com.erp.procurement.entity.AuditLog;
import com.erp.procurement.enums.AuditAction;
import com.erp.procurement.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void log(AuditAction action, String entityType, Long entityId,
                    String performedBy, String previousStatus, String newStatus, String description) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getLogsForEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
    }
}
