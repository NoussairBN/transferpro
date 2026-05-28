package ma.transfert.dao;

import ma.transfert.model.Transfer;
import ma.transfert.model.enums.TransferStatus;
import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class TransferDAO extends BaseDAO<Transfer, Long> {
    
    public TransferDAO() {
        super(Transfer.class);
    }
    
    public Transfer findByTrackingCode(String trackingCode) {
        try {
            return em.createNamedQuery("Transfer.findByTrackingCode", Transfer.class)
                    .setParameter("code", trackingCode)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public List<Transfer> findByStatus(TransferStatus status, int maxResults) {
        TypedQuery<Transfer> query = em.createNamedQuery("Transfer.findByStatus", Transfer.class);
        query.setParameter("status", status);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }
    
    public List<Transfer> findByAgency(Long agencyId, int offset, int limit) {
        return em.createQuery(
            "SELECT t FROM Transfer t WHERE t.sendingAgency.id = :agencyId ORDER BY t.createdAt DESC",
            Transfer.class)
            .setParameter("agencyId", agencyId)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }
    
    public List<Transfer> findByUser(Long userId, int offset, int limit) {
        return em.createQuery(
            "SELECT t FROM Transfer t WHERE t.user.id = :userId ORDER BY t.createdAt DESC",
            Transfer.class)
            .setParameter("userId", userId)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }
    
    public long countByStatus(TransferStatus status) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Transfer t WHERE t.status = :status", Long.class);
        query.setParameter("status", status);
        return query.getSingleResult();
    }
    
    public BigDecimal getTotalVolume() {
        TypedQuery<BigDecimal> query = em.createQuery(
            "SELECT SUM(t.amount) FROM Transfer t WHERE t.status = :status", BigDecimal.class);
        query.setParameter("status", TransferStatus.PAID);
        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}