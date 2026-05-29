package ma.transfert.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour PasswordUtil (BCrypt).
 * Aucune dépendance JEE — test pur Java.
 */
@DisplayName("T1 — PasswordUtil (BCrypt)")
class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    @Test
    @DisplayName("hash() retourne un hash non-null et non-vide")
    void hash_returnNonNullHash() {
        String hash = passwordUtil.hash("motdepasse123");
        assertNotNull(hash);
        assertFalse(hash.isBlank());
    }

    @Test
    @DisplayName("hash() retourne un hash BCrypt (commence par $2a$)")
    void hash_returnBcryptFormat() {
        String hash = passwordUtil.hash("motdepasse123");
        assertTrue(hash.startsWith("$2a$"), "Le hash doit être au format BCrypt");
    }

    @Test
    @DisplayName("Deux hash du même mot de passe sont différents (salt aléatoire)")
    void hash_differentHashesForSamePassword() {
        String hash1 = passwordUtil.hash("motdepasse123");
        String hash2 = passwordUtil.hash("motdepasse123");
        assertNotEquals(hash1, hash2, "BCrypt génère un salt différent à chaque fois");
    }

    @Test
    @DisplayName("verify() retourne true pour le bon mot de passe")
    void verify_correctPassword_returnsTrue() {
        String hash = passwordUtil.hash("motdepasse123");
        assertTrue(passwordUtil.verify("motdepasse123", hash));
    }

    @Test
    @DisplayName("verify() retourne false pour un mauvais mot de passe")
    void verify_wrongPassword_returnsFalse() {
        String hash = passwordUtil.hash("motdepasse123");
        assertFalse(passwordUtil.verify("mauvaismdp", hash));
    }

    @Test
    @DisplayName("verify() retourne false si le mot de passe est null")
    void verify_nullPassword_returnsFalse() {
        String hash = passwordUtil.hash("motdepasse123");
        assertFalse(passwordUtil.verify(null, hash));
    }

    @Test
    @DisplayName("verify() retourne false si le hash est null")
    void verify_nullHash_returnsFalse() {
        assertFalse(passwordUtil.verify("motdepasse123", null));
    }

    @Test
    @DisplayName("hash() lève IllegalArgumentException pour un mot de passe vide")
    void hash_emptyPassword_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hash(""));
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hash("   "));
        assertThrows(IllegalArgumentException.class, () -> passwordUtil.hash(null));
    }
}
