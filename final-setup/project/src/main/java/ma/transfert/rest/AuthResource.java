package ma.transfert.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.transfert.exception.BusinessException;
import ma.transfert.service.AuthService;
import ma.transfert.service.AuthService.LoginResult;

import java.util.Map;

/**
 * DEV-3 — Endpoints REST d'authentification.
 *
 * Routes publiques (pas de JWT requis) :
 *   POST /api/auth/login    → connexion
 *   POST /api/auth/register → inscription
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @EJB
    private AuthService authService;

    // ─────────────────────────────────────────────────────────────────────────
    // DTO internes (inline, simples pour éviter la surcharge de fichiers)
    // ─────────────────────────────────────────────────────────────────────────

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class RegisterRequest {
        public String firstName;
        public String lastName;
        public String email;
        public String phone;
        public String password;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Authentifie un utilisateur.
     *
     * Corps JSON attendu :
     * {@code { "email": "...", "password": "..." }}
     *
     * Réponse 200 :
     * {@code { "token": "jwt...", "userId": 1, "email": "...", "fullName": "...", "role": "..." }}
     *
     * Réponse 401 si identifiants incorrects.
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        if (req == null || req.email == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Requête invalide : email et password requis"))
                    .build();
        }

        try {
            LoginResult result = authService.login(req.email, req.password);
            return Response.ok(loginResultToMap(result)).build();

        } catch (BusinessException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(error(e.getMessage()))
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inscrit un nouvel utilisateur et le connecte automatiquement.
     *
     * Corps JSON attendu :
     * {@code { "firstName": "...", "lastName": "...", "email": "...", "phone": "...", "password": "..." }}
     *
     * Réponse 201 avec le token JWT.
     * Réponse 400 si données invalides ou email/téléphone déjà utilisé.
     */
    @POST
    @Path("/register")
    public Response register(RegisterRequest req) {
        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Corps de requête manquant"))
                    .build();
        }

        try {
            LoginResult result = authService.register(
                    req.firstName, req.lastName, req.email, req.phone, req.password
            );
            return Response.status(Response.Status.CREATED)
                    .entity(loginResultToMap(result))
                    .build();

        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage()))
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> loginResultToMap(LoginResult r) {
        return Map.of(
                "token",    r.getToken(),
                "userId",   r.getUserId(),
                "email",    r.getEmail(),
                "fullName", r.getFullName(),
                "role",     r.getRole()
        );
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
