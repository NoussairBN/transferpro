package ma.transfert.service;

import jakarta.inject.Inject;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import ma.transfert.dao.UserDAO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.User;
import ma.transfert.security.JWTUtil;
import ma.transfert.security.PasswordUtil;

import java.time.LocalDateTime;

/**
 * DEV-3 — Service d'authentification.
 * Responsabilités : login (email/password → JWT), register via UserService.
 * Ne gère pas directement le profil (délégué à UserService).
 */
@Stateless
public class AuthService {

    @EJB
    private UserDAO userDAO;

    @Inject
    private PasswordUtil passwordUtil;

    @Inject
    private JWTUtil jwtUtil;
    
    @EJB
    private UserService userService;

    /**
     * Résultat d'une authentification réussie.
     * Contient le token JWT et les informations essentielles de l'utilisateur.
     */
    public static class LoginResult {
        private final String token;
        private final Long userId;
        private final String email;
        private final String fullName;
        private final String role;

        public LoginResult(String token, Long userId, String email, String fullName, String role) {
            this.token    = token;
            this.userId   = userId;
            this.email    = email;
            this.fullName = fullName;
            this.role     = role;
        }

        public String getToken()    { return token;    }
        public Long   getUserId()   { return userId;   }
        public String getEmail()    { return email;    }
        public String getFullName() { return fullName; }
        public String getRole()     { return role;     }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Authentifie un utilisateur et retourne un token JWT.
     *
     * Règles de sécurité :
     * - Message d'erreur volontairement générique (pas de distinction email/mdp)
     *   pour éviter l'énumération de comptes.
     * - Mise à jour de lastLoginAt après succès.
     *
     * @param email    email saisi
     * @param password mot de passe en clair saisi
     * @return un {@link LoginResult} contenant le JWT et les infos utilisateur
     * @throws BusinessException si l'email/mot de passe est incorrect ou le compte inactif
     */
    public LoginResult login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email et mot de passe requis");
        }
        if (password == null || password.isBlank()) {
            throw new BusinessException("Email et mot de passe requis");
        }

        // Recherche de l'utilisateur (message générique pour sécurité)
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            throw new BusinessException("Email ou mot de passe incorrect");
        }

        // Vérification du mot de passe
        if (!passwordUtil.verify(password, user.getPasswordHash())) {
            throw new BusinessException("Email ou mot de passe incorrect");
        }

        // Vérification du statut du compte
        if (user.getStatus() == User.AccountStatus.SUSPENDED) {
            throw new BusinessException("Votre compte est suspendu. Contactez le support.");
        }
        if (user.getStatus() == User.AccountStatus.BLOCKED) {
            throw new BusinessException("Votre compte est bloqué. Contactez le support.");
        }

        // Mise à jour de la date de dernière connexion
        user.setLastLoginAt(LocalDateTime.now());
        userDAO.update(user);

        // Génération du token JWT
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFullName()
        );

        return new LoginResult(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inscrit un nouvel utilisateur et retourne directement un token JWT
     * (connexion automatique après inscription).
     *
     * @param firstName prénom
     * @param lastName  nom
     * @param email     email unique
     * @param phone     téléphone unique
     * @param password  mot de passe en clair
     * @return un {@link LoginResult} avec le token JWT généré
     * @throws BusinessException si les données sont invalides ou l'email/téléphone déjà pris
     */
    public LoginResult register(String firstName, String lastName,
                                String email, String phone,
                                String password) {

        // Délégation à UserService pour la logique de création
        User newUser = userService.register(
                firstName, lastName, email, phone, password, User.UserRole.INDIVIDUAL
        );

        // Connexion automatique : génération du token
        String token = jwtUtil.generateToken(
                newUser.getId(),
                newUser.getEmail(),
                newUser.getRole().name(),
                newUser.getFullName()
        );

        return new LoginResult(token, newUser.getId(), newUser.getEmail(),
                newUser.getFullName(), newUser.getRole().name());
    }
}
