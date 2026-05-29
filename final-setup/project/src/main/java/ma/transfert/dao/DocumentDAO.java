package ma.transfert.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.transfert.model.Document;
import ma.transfert.model.DocumentStatus;
import ma.transfert.model.DocumentType;

import java.time.LocalDateTime;
import java.util.List;

@Stateless
public class DocumentDAO {

    @PersistenceContext
    private EntityManager em;

    public Document save(Document document) {
        em.persist(document);
        return document;
    }

    public Document findById(Long id) {
        return em.find(Document.class, id);
    }

    public Document update(Document document) {
        return em.merge(document);
    }

    public void delete(Long id) {
        Document doc = findById(id);
        if (doc != null) em.remove(doc);
    }

    // ✅ NOUVEAU : utilise owner.id (relation JPA) au lieu de ownerId brut
    public List<Document> findByOwner(Long userId) {
        return em.createQuery(
                "SELECT d FROM Document d WHERE d.owner.id = :userId ORDER BY d.uploadedAt DESC",
                Document.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // ✅ NOUVEAU : filtrer par type pour un utilisateur
    public List<Document> findByOwnerAndType(Long userId, DocumentType type) {
        return em.createQuery(
                "SELECT d FROM Document d WHERE d.owner.id = :userId AND d.type = :type",
                Document.class)
                .setParameter("userId", userId)
                .setParameter("type", type)
                .getResultList();
    }

    // ✅ NOUVEAU : tous les docs en attente (vue admin KYC)
    public List<Document> findByStatus(DocumentStatus status) {
        return em.createQuery(
                "SELECT d FROM Document d WHERE d.status = :status ORDER BY d.uploadedAt ASC",
                Document.class)
                .setParameter("status", status)
                .getResultList();
    }

    // ✅ NOUVEAU : documents liés à un transfert (DEV-2)
    public List<Document> findByTransfer(Long transferId) {
        return em.createQuery(
                "SELECT d FROM Document d WHERE d.transfer.id = :transferId",
                Document.class)
                .setParameter("transferId", transferId)
                .getResultList();
    }
}