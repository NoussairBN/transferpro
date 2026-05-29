package ma.transfert.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import ma.transfert.dao.UserDAO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.User;
import ma.transfert.model.User.AccountStatus;
import ma.transfert.security.PasswordUtil;

import java.util.List;

/**
 * DEV-3 — Service de gestion des utilisateurs.
 * Responsabilités : inscription, consultation, mise à jour du profil,
 * changement de mot de passe, activation/suspension de compte.
 */
@Stateless
public class UserService {

    @EJB
    private UserDAO userDAO;

    @EJB
    private PasswordUtil passwordUtil;

    // ─────────────────────────────────────────────────────────────────────────
    // INSCRIPTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inscrit un nouvel utilisateur après validation des données.
     *
     * @param firstName prénom
     * @param lastName  nom
     * @param email     email unique
     * @param phone     téléphone unique
     * @param password  mot de passe en clair (sera haché)
     * @param role      rôle applicatif
     * @return l'utilisateur persisté
     * @throws BusinessException si l'email ou le téléphone est déjà utilisé
     */
    public User register(String firstName, String lastName,
                         String email, String phone,
                         String password, User.UserRole role) {

        // Validation des champs obligatoires
        if (firstName == null || firstName.isBlank()) {
            throw new BusinessException("Le prénom est obligatoire");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new BusinessException("Le nom est obligatoire");
        }
        if (email == null || !email.contains("@")) {
            throw new BusinessException("Email invalide");
        }
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Le téléphone est obligatoire");
        }
        if (password == null || password.length() < 8) {
            throw new BusinessException("Le mot de passe doit contenir au moins 8 caractères");
        }

        // Unicité email
        if (userDAO.findByEmail(email) != null) {
            throw new BusinessException("Cet email est déjà utilisé");
        }

        // Unicité téléphone
        if (userDAO.findByPhone(phone) != null) {
            throw new BusinessException("Ce numéro de téléphone est déjà utilisé");
        }

        // Construction de l'entité
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone.trim());
        user.setPasswordHash(passwordUtil.hash(password));
        user.setRole(role != null ? role : User.UserRole.INDIVIDUAL);
        user.setStatus(AccountStatus.ACTIVE);
        user.setKycStatus(User.KycStatus.PENDING);

        return userDAO.save(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Récupère un utilisateur par son identifiant.
     *
     * @param id identifiant de l'utilisateur
     * @return l'utilisateur trouvé
     * @throws BusinessException si l'utilisateur n'existe pas
     */
    public User findById(Long id) {
        User user = userDAO.findById(id);
        if (user == null) {
            throw new BusinessException("Utilisateur introuvable (id=" + id + ")");
        }
        return user;
    }

    /**
     * Récupère un utilisateur par son email.
     *
     * @param email adresse email
     * @return l'utilisateur trouvé
     * @throws BusinessException si l'utilisateur n'existe pas
     */
    public User findByEmail(String email) {
        User user = userDAO.findByEmail(email);
        if (user == null) {
            throw new BusinessException("Aucun compte trouvé pour cet email");
        }
        return user;
    }

    /**
     * Retourne tous les utilisateurs actifs.
     */
    public List<User> findAllActive() {
        return userDAO.findAllActive();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MISE À JOUR DU PROFIL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Met à jour le prénom, le nom et/ou le téléphone d'un utilisateur.
     *
     * @param userId    identifiant de l'utilisateur
     * @param firstName nouveau prénom (null = pas de changement)
     * @param lastName  nouveau nom (null = pas de changement)
     * @param phone     nouveau téléphone (null = pas de changement)
     * @return l'utilisateur mis à jour
     * @throws BusinessException si le téléphone est déjà pris par un autre compte
     */
    public User updateProfile(Long userId, String firstName, String lastName, String phone) {
        User user = findById(userId);

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName.trim());
        }
        if (phone != null && !phone.isBlank()) {
            User existing = userDAO.findByPhone(phone.trim());
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException("Ce numéro de téléphone est déjà utilisé par un autre compte");
            }
            user.setPhone(phone.trim());
        }

        return userDAO.update(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOT DE PASSE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Change le mot de passe d'un utilisateur après vérification de l'ancien.
     *
     * @param userId      identifiant de l'utilisateur
     * @param oldPassword ancien mot de passe en clair
     * @param newPassword nouveau mot de passe en clair
     * @throws BusinessException si l'ancien mot de passe est incorrect ou le nouveau trop court
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);

        if (!passwordUtil.verify(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Mot de passe actuel incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("Le nouveau mot de passe doit contenir au moins 8 caractères");
        }

        user.setPasswordHash(passwordUtil.hash(newPassword));
        userDAO.update(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN — GESTION DES STATUTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Suspend un compte utilisateur (admin uniquement).
     *
     * @param userId identifiant de l'utilisateur à suspendre
     */
    public void suspendUser(Long userId) {
        User user = findById(userId);
        user.setStatus(AccountStatus.SUSPENDED);
        userDAO.update(user);
    }

    /**
     * Réactive un compte utilisateur suspendu (admin uniquement).
     *
     * @param userId identifiant de l'utilisateur
     */
    public void activateUser(Long userId) {
        User user = findById(userId);
        user.setStatus(AccountStatus.ACTIVE);
        userDAO.update(user);
    }
}
