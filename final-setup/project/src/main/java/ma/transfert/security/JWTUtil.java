package ma.transfert.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * DEV-3 — Utilitaire JWT : génération et validation des tokens d'accès.
 * Utilise HMAC256 avec une clé secrète configurée via variable d'environnement.
 * Durée de validité : 24 heures.
 */
@ApplicationScoped
public class JWTUtil {

    /** Clé secrète lue depuis l'env, avec fallback sécurisé pour les tests. */
    private static final String SECRET_KEY = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "transferpro-dev3-secret-key-change-in-prod-min32chars!";

    private static final String ISSUER       = "TransferPro";
    private static final long   EXPIRY_HOURS = 24;

    /** Claims personnalisés embarqués dans le token */
    public static final String CLAIM_ROLE     = "role";
    public static final String CLAIM_EMAIL    = "email";
    public static final String CLAIM_FULL_NAME = "fullName";

    /**
     * Génère un token JWT signé pour un utilisateur authentifié.
     *
     * @param userId   identifiant de l'utilisateur en base
     * @param email    email de l'utilisateur (subject JWT)
     * @param role     rôle applicatif (ex: "INDIVIDUAL", "ADMIN")
     * @param fullName prénom + nom pour affichage côté client
     * @return le token JWT signé (String)
     */
    public String generateToken(Long userId, String email, String role, String fullName) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(userId))
                .withClaim(CLAIM_EMAIL, email)
                .withClaim(CLAIM_ROLE, role)
                .withClaim(CLAIM_FULL_NAME, fullName)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(EXPIRY_HOURS, ChronoUnit.HOURS)))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    /**
     * Valide un token JWT et retourne ses données décodées.
     *
     * @param token le token JWT brut (sans "Bearer ")
     * @return le token décodé avec tous ses claims
     * @throws JWTVerificationException si le token est invalide, expiré ou malformé
     */
    public DecodedJWT verify(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY))
                .withIssuer(ISSUER)
                .build()
                .verify(token);
    }

    /**
     * Extrait l'ID utilisateur (subject) d'un token déjà vérifié.
     *
     * @param decoded le token décodé par {@link #verify(String)}
     * @return l'ID utilisateur
     */
    public Long extractUserId(DecodedJWT decoded) {
        return Long.parseLong(decoded.getSubject());
    }

    /**
     * Extrait le rôle d'un token déjà vérifié.
     *
     * @param decoded le token décodé
     * @return le rôle applicatif (String)
     */
    public String extractRole(DecodedJWT decoded) {
        return decoded.getClaim(CLAIM_ROLE).asString();
    }

    /**
     * Extrait l'email d'un token déjà vérifié.
     *
     * @param decoded le token décodé
     * @return l'adresse email
     */
    public String extractEmail(DecodedJWT decoded) {
        return decoded.getClaim(CLAIM_EMAIL).asString();
    }

    /**
     * Vérifie si un token brut est valide sans lever d'exception.
     *
     * @param token le token JWT brut
     * @return true si valide et non expiré, false sinon
     */
    public boolean isValid(String token) {
        try {
            verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
