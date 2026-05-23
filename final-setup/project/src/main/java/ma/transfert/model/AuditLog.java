package ma.transfert.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created", columnList = "createdAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String action;          // Ex: TRANSFER_CREATED, LOGIN_SUCCESS, KYC_VERIFIED

    @Column(length = 50)
    private String entityType;      // Ex: Transfer, User, Agency

    @Column
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String details;         // JSON des détails

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Factory methods ──────────────────────────────────────────
    public static AuditLog of(User user, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.user = user;
        log.action = action;
        log.entityType = entityType;
        log.entityId = entityId;
        log.details = details;
        return log;
    }

    // ── Getters ───────────────────────────────────────────────────
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
