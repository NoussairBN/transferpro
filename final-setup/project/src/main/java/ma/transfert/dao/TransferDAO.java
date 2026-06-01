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
    public List<Transfer> findAll(int offset, int limit) {
        return em.createQuery(
        "SELECT t FROM Transfer t ORDER BY t.createdAt DESC",
        Transfer.class)
        .setFirstResult(offset)
        .setMaxResults(limit)
        .getResultList();
    }

    // ─── DEV-4 : Statistiques par agence (Dashboard) ────────────────────────

    /**
     * Compte les transferts d'une agence, filtrés par statut (envoi ou réception).
     */
    public long countByAgencyAndStatus(Long agencyId, TransferStatus status) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Transfer t WHERE (t.sendingAgency.id = :agencyId OR t.receivingAgency.id = :agencyId) AND t.status = :status",
            Long.class);
        query.setParameter("agencyId", agencyId);
        query.setParameter("status", status);
        return query.getSingleResult();
    }

    /**
     * Compte tous les transferts d'une agence (envoi + réception).
     */
    public long countAllByAgency(Long agencyId) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(t) FROM Transfer t WHERE t.sendingAgency.id = :agencyId OR t.receivingAgency.id = :agencyId",
            Long.class);
        query.setParameter("agencyId", agencyId);
        return query.getSingleResult();
    }

    /**
     * Volume total envoyé par une agence (transferts payés).
     */
    public BigDecimal getTotalVolumeSentByAgency(Long agencyId) {
        TypedQuery<BigDecimal> query = em.createQuery(
            "SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.sendingAgency.id = :agencyId AND t.status = :status",
            BigDecimal.class);
        query.setParameter("agencyId", agencyId);
        query.setParameter("status", TransferStatus.PAID);
        return query.getSingleResult();
    }

    /**
     * Volume total reçu par une agence (transferts payés).
     */
    public BigDecimal getTotalVolumeReceivedByAgency(Long agencyId) {
        TypedQuery<BigDecimal> query = em.createQuery(
            "SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t WHERE t.receivingAgency.id = :agencyId AND t.status = :status",
            BigDecimal.class);
        query.setParameter("agencyId", agencyId);
        query.setParameter("status", TransferStatus.PAID);
        return query.getSingleResult();
    }

    /**
     * Total des frais collectés par une agence (transferts envoyés et payés).
     */
    public BigDecimal getTotalFeesByAgency(Long agencyId) {
        TypedQuery<BigDecimal> query = em.createQuery(
            "SELECT COALESCE(SUM(t.fees), 0) FROM Transfer t WHERE t.sendingAgency.id = :agencyId AND t.status = :status",
            BigDecimal.class);
        query.setParameter("agencyId", agencyId);
        query.setParameter("status", TransferStatus.PAID);
        return query.getSingleResult();
    }
}