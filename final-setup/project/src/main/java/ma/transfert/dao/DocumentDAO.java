package ma.transfert.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.transfert.model.Document;
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

    public List<Document> findByOwnerId(Long ownerId) {
        return em.createQuery(
                "SELECT d FROM Document d WHERE d.ownerId = :ownerId",
                Document.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }
}
