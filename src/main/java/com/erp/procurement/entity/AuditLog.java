package com.erp.procurement.entity;

import com.erp.procurement.enums.AuditAction;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
        @Index(name = "idx_audit_user",   columnList = "performedBy"),
        @Index(name = "idx_audit_time",   columnList = "performedAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column
    private Long entityId;

    @Column(length = 100)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 30)
    private String previousStatus;

    @Column(length = 30)
    private String newStatus;

    @Column(length = 1000)
    private String description;

    @Column(length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() { this.performedAt = LocalDateTime.now(); }

    public AuditLog() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AuditLog log = new AuditLog();
        public Builder action(AuditAction v)        { log.action = v; return this; }
        public Builder entityType(String v)         { log.entityType = v; return this; }
        public Builder entityId(Long v)             { log.entityId = v; return this; }
        public Builder performedBy(String v)        { log.performedBy = v; return this; }
        public Builder previousStatus(String v)     { log.previousStatus = v; return this; }
        public Builder newStatus(String v)          { log.newStatus = v; return this; }
        public Builder description(String v)        { log.description = v; return this; }
        public AuditLog build()                     { return log; }
    }
}
