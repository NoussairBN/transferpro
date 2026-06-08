package ma.transfert.dao;

import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;
import ma.transfert.model.User;

@Stateless
public class UserDAO extends BaseDAO<User, Long> {
    
    public UserDAO() {
        super(User.class);
    }
    
    public User findByEmail(String email) {
        try {
            return em.createNamedQuery("User.findByEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public User findByPhone(String phone) {
        try {
            return em.createNamedQuery("User.findByPhone", User.class)
                    .setParameter("phone", phone)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public List<User> findAllActive() {
        return em.createNamedQuery("User.findAllActive", User.class)
                .getResultList();
    }
    
    public List<User> findByAgency(Long agencyId) {
        return em.createQuery(
            "SELECT u FROM User u WHERE u.agency.id = :agencyId",
            User.class)
            .setParameter("agencyId", agencyId)
            .getResultList();
    }
    
    public User findByEmailWithAgency(String email) {
        try {
            return em.createQuery(
                "SELECT u FROM User u LEFT JOIN FETCH u.agency WHERE u.email = :email",
                User.class)
                .setParameter("email", email)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}