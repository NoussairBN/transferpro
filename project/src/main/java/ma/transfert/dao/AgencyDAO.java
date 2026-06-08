package ma.transfert.dao;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import ma.transfert.model.Agency;

@Stateless
public class AgencyDAO extends BaseDAO<Agency, Long> {
    
    public AgencyDAO() {
        super(Agency.class);
    }
    
    public Agency findByCode(String code) {
        try {
            return em.createNamedQuery("Agency.findByCode", Agency.class)
                    .setParameter("code", code)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public List<Agency> findAllActive() {
        return em.createNamedQuery("Agency.findAllActive", Agency.class)
                .getResultList();
    }
    
    public Agency findByIdWithAgents(Long id) {
        try {
            return em.createQuery(
                "SELECT DISTINCT a FROM Agency a LEFT JOIN FETCH a.agents WHERE a.id = :id",
                Agency.class)
                .setParameter("id", id)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}