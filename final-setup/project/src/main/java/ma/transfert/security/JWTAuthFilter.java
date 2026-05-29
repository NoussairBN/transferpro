package ma.transfert.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;

/**
 * DEV-3 — Filtre JAX-RS qui intercepte chaque requête HTTP.
 *
 * Comportement :
 * - Si le header "Authorization: Bearer <token>" est présent ET valide,
 *   injecte un SecurityContext personnalisé avec l'identité et le rôle.
 * - Si le token est absent ou invalide, la requête CONTINUE sans identité
 *   (les endpoints @PermitAll n'ont pas besoin de token).
 * - Les endpoints sécurisés doivent vérifier eux-mêmes le SecurityContext
 *   ou utiliser @RolesAllowed (via un sous-filtre dédié si besoin).
 *
 * Priorité AUTHENTICATION : s'exécute avant tous les autres filtres.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTAuthFilter implements ContainerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    private JWTUtil jwtUtil;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Pas de header → requête publique (health, track, register, login)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            DecodedJWT decoded = jwtUtil.verify(token);

            Long userId   = jwtUtil.extractUserId(decoded);
            String email  = jwtUtil.extractEmail(decoded);
            String role   = jwtUtil.extractRole(decoded);

            // Injecter le SecurityContext avec l'identité extraite du JWT
            requestContext.setSecurityContext(buildSecurityContext(userId, email, role));

        } catch (JWTVerificationException e) {
            // Token invalide ou expiré → refus avec 401
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Token invalide ou expiré\"}")
                    .type("application/json")
                    .build()
            );
        }
    }

    /**
     * Construit un {@link SecurityContext} JAX-RS à partir des données JWT.
     */
    private SecurityContext buildSecurityContext(Long userId, String email, String role) {
        return new SecurityContext() {

            @Override
            public Principal getUserPrincipal() {
                return () -> String.valueOf(userId); // Principal = userId
            }

            @Override
            public boolean isUserInRole(String r) {
                return role != null && role.equals(r);
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return "Bearer";
            }
        };
    }
}
