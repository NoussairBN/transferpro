package ma.transfert.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour JWTUtil.
 * Aucune dépendance JEE — test pur Java.
 */
@DisplayName("T2 — JWTUtil (JWT Auth0)")
class JWTUtilTest {

    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JWTUtil();
    }

    @Test
    @DisplayName("generateToken() retourne un token non-null et non-vide")
    void generateToken_returnsNonNull() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "INDIVIDUAL", "Hassan Alami");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("generateToken() retourne un JWT en 3 parties séparées par des points")
    void generateToken_returnsJwtFormat() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "INDIVIDUAL", "Hassan Alami");
        assertEquals(3, token.split("\\.").length, "Un JWT doit avoir 3 parties: header.payload.signature");
    }

    @Test
    @DisplayName("verify() valide un token généré par generateToken()")
    void verify_validToken_noException() {
        String token = jwtUtil.generateToken(42L, "test@example.com", "ADMIN", "Super Admin");
        assertDoesNotThrow(() -> jwtUtil.verify(token));
    }

    @Test
    @DisplayName("verify() lève une exception pour un token falsifié")
    void verify_tamperedToken_throwsException() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "INDIVIDUAL", "Hassan");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThrows(JWTVerificationException.class, () -> jwtUtil.verify(tampered));
    }

    @Test
    @DisplayName("extractUserId() retourne l'ID correct depuis le token")
    void extractUserId_returnsCorrectId() {
        String token = jwtUtil.generateToken(99L, "test@example.com", "INDIVIDUAL", "Test User");
        DecodedJWT decoded = jwtUtil.verify(token);
        assertEquals(99L, jwtUtil.extractUserId(decoded));
    }

    @Test
    @DisplayName("extractRole() retourne le rôle correct depuis le token")
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "AGENCY_AGENT", "Agent Agadir");
        DecodedJWT decoded = jwtUtil.verify(token);
        assertEquals("AGENCY_AGENT", jwtUtil.extractRole(decoded));
    }

    @Test
    @DisplayName("extractEmail() retourne l'email correct depuis le token")
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken(1L, "hassan@transferpro.ma", "INDIVIDUAL", "Hassan");
        DecodedJWT decoded = jwtUtil.verify(token);
        assertEquals("hassan@transferpro.ma", jwtUtil.extractEmail(decoded));
    }

    @Test
    @DisplayName("isValid() retourne true pour un token valide")
    void isValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, "test@example.com", "INDIVIDUAL", "Test");
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    @DisplayName("isValid() retourne false pour un token corrompu")
    void isValid_corruptToken_returnsFalse() {
        assertFalse(jwtUtil.isValid("pas.un.jwt.valide"));
    }

    @Test
    @DisplayName("isValid() retourne false pour null")
    void isValid_null_returnsFalse() {
        assertFalse(jwtUtil.isValid(null));
    }
}
