package ma.transfert.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import java.io.Serializable;
import java.util.List;

public abstract class BaseDAO<T, ID extends Serializable> {
    
    @PersistenceContext(unitName = "MoneyTransferPU")
    protected EntityManager em;
    
    private final Class<T> entityClass;
    
    public BaseDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }
    
    public T save(T entity) {
        em.persist(entity);
        return entity;
    }
    
    public T update(T entity) {
        return em.merge(entity);
    }
    
    public void delete(T entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }
    
    public T findById(ID id) {
        return em.find(entityClass, id);
    }
    
    public List<T> findAll() {
        CriteriaQuery<T> cq = em.getCriteriaBuilder().createQuery(entityClass);
        cq.select(cq.from(entityClass));
        return em.createQuery(cq).getResultList();
    }
    
    public long count() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        cq.select(cb.count(cq.from(entityClass)));
        return em.createQuery(cq).getSingleResult();
    }
}