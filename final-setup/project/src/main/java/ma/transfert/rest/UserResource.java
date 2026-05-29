package ma.transfert.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.User;
import ma.transfert.service.UserService;

import java.util.Map;

/**
 * DEV-3 — Endpoints REST de gestion du profil utilisateur.
 *
 * Toutes ces routes nécessitent un token JWT valide (via JWTAuthFilter).
 *
 *   GET    /api/users/me              → profil de l'utilisateur connecté
 *   PUT    /api/users/me              → mise à jour du profil
 *   POST   /api/users/me/password     → changement de mot de passe
 *   GET    /api/users/{id}            → profil d'un autre utilisateur (admin)
 *   POST   /api/users/{id}/suspend    → suspendre un compte (admin)
 *   POST   /api/users/{id}/activate   → réactiver un compte (admin)
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @EJB
    private UserService userService;

    // ─── DTOs inline ──────────────────────────────────────────────────────────

    public static class UpdateProfileRequest {
        public String firstName;
        public String lastName;
        public String phone;
    }

    public static class ChangePasswordRequest {
        public String oldPassword;
        public String newPassword;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users/me
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne le profil de l'utilisateur authentifié.
     * Nécessite : Authorization: Bearer {token}
     */
    @GET
    @Path("/me")
    public Response getMyProfile(@Context SecurityContext sc) {
        Long userId = extractUserId(sc);
        if (userId == null) {
            return unauthorized();
        }
        try {
            User user = userService.findById(userId);
            return Response.ok(userToMap(user)).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/users/me
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Met à jour le prénom, nom ou téléphone de l'utilisateur connecté.
     */
    @PUT
    @Path("/me")
    public Response updateMyProfile(@Context SecurityContext sc, UpdateProfileRequest req) {
        Long userId = extractUserId(sc);
        if (userId == null) return unauthorized();
        if (req == null)    return badRequest("Corps de requête manquant");

        try {
            User updated = userService.updateProfile(userId, req.firstName, req.lastName, req.phone);
            return Response.ok(userToMap(updated)).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/users/me/password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Change le mot de passe de l'utilisateur connecté.
     * Nécessite l'ancien mot de passe pour validation.
     */
    @POST
    @Path("/me/password")
    public Response changePassword(@Context SecurityContext sc, ChangePasswordRequest req) {
        Long userId = extractUserId(sc);
        if (userId == null) return unauthorized();
        if (req == null || req.oldPassword == null || req.newPassword == null) {
            return badRequest("oldPassword et newPassword requis");
        }

        try {
            userService.changePassword(userId, req.oldPassword, req.newPassword);
            return Response.ok(Map.of("message", "Mot de passe mis à jour avec succès")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/users/{id}  (admin)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne le profil d'un utilisateur par son ID.
     * Réservé aux admins.
     */
    @GET
    @Path("/{id}")
    public Response getUserById(@Context SecurityContext sc, @PathParam("id") Long id) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            User user = userService.findById(id);
            return Response.ok(userToMap(user)).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/users/{id}/suspend  (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @POST
    @Path("/{id}/suspend")
    public Response suspendUser(@Context SecurityContext sc, @PathParam("id") Long id) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            userService.suspendUser(id);
            return Response.ok(Map.of("message", "Compte suspendu")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/users/{id}/activate  (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @POST
    @Path("/{id}/activate")
    public Response activateUser(@Context SecurityContext sc, @PathParam("id") Long id) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            userService.activateUser(id);
            return Response.ok(Map.of("message", "Compte réactivé")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Extrait l'userId depuis le SecurityContext injecté par JWTAuthFilter. */
    private Long extractUserId(SecurityContext sc) {
        if (sc == null || sc.getUserPrincipal() == null) return null;
        try {
            return Long.parseLong(sc.getUserPrincipal().getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Vérifie si l'utilisateur authentifié a le rôle ADMIN. */
    private boolean isAdmin(SecurityContext sc) {
        return sc != null && sc.isUserInRole("ADMIN");
    }

    /** Convertit un User en Map JSON-friendly (sans passwordHash). */
    private Map<String, Object> userToMap(User u) {
        return Map.of(
                "id",        u.getId(),
                "firstName", u.getFirstName(),
                "lastName",  u.getLastName(),
                "email",     u.getEmail(),
                "phone",     u.getPhone() != null ? u.getPhone() : "",
                "role",      u.getRole().name(),
                "kycStatus", u.getKycStatus().name(),
                "status",    u.getStatus().name()
        );
    }

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(error("Authentification requise")).build();
    }

    private Response badRequest(String msg) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(error(msg)).build();
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
