package ma.transfert.service;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.transfert.model.AuditLog;
import ma.transfert.model.User;

@Stateless
public class AuditLogService {

    @PersistenceContext(unitName = "MoneyTransferPU")
    private EntityManager em;

    @Asynchronous
    public void log(User user, String action, String entityType, Long entityId, String details) {
        AuditLog entry = AuditLog.of(user, action, entityType, entityId, details);
        em.persist(entry);
    }

    @Asynchronous
    public void logWithIp(User user, String action, String entityType, Long entityId,
                          String details, String ipAddress) {
        AuditLog entry = AuditLog.of(user, action, entityType, entityId, details);
        entry.setIpAddress(ipAddress);
        em.persist(entry);
    }

    @Asynchronous
    public void logSystem(String action, String entityType, Long entityId, String details) {
        log(null, action, entityType, entityId, details);
    }
}