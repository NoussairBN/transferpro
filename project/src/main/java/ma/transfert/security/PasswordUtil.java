package ma.transfert.security;

import org.mindrot.jbcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * DEV-3 — Utilitaire pour le hachage et la vérification des mots de passe.
 * Utilise BCrypt (jBCrypt 0.4) avec un facteur de coût de 12.
 */
@ApplicationScoped
public class PasswordUtil {

    private static final int BCRYPT_COST = 12;

    /**
     * Hache un mot de passe en clair avec BCrypt.
     *
     * @param plainPassword le mot de passe en clair (jamais null)
     * @return le hash BCrypt prêt à stocker en base
     * @throws IllegalArgumentException si le mot de passe est null ou vide
     */
    public String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Vérifie qu'un mot de passe en clair correspond à son hash stocké.
     *
     * @param plainPassword  le mot de passe saisi par l'utilisateur
     * @param hashedPassword le hash stocké en base
     * @return true si le mot de passe correspond, false sinon
     */
    public boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Hash invalide/corrompu en base
            return false;
        }
    }
}