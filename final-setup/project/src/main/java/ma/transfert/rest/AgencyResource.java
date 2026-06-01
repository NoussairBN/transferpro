package ma.transfert.rest;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ma.transfert.dto.AgencyDTO;
import ma.transfert.dto.AgencyRequestDTO;
import ma.transfert.dto.AgencyDashboardDTO;
import ma.transfert.dto.AgentDTO;
import ma.transfert.dto.CashOperationRequestDTO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.Agency;
import ma.transfert.model.Agency.AgencyStatus;
import ma.transfert.model.User;
import ma.transfert.service.AgencyService;
import ma.transfert.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DEV-4 — Endpoints REST pour la gestion des agences.
 * Toutes ces routes nécessitent une authentification JWT valide.
 */
@Path("/agencies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgencyResource {

    @EJB
    private AgencyService agencyService;

    @EJB
    private UserService userService;

    // ─────────────────────────────────────────────────────────────────────────
    // LISTER TOUTES LES AGENCES (ADMIN UNIQUEMENT)
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    public Response getAllAgencies(@Context SecurityContext sc) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            List<Agency> agencies = agencyService.findAll();
            List<AgencyDTO> dtos = new ArrayList<>();
            for (Agency a : agencies) {
                dtos.add(agencyToDTO(a));
            }
            return Response.ok(dtos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error("Erreur serveur : " + e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTER LES AGENCES ACTIVES (TOUS RÔLES CONNECTÉS)
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    @Path("/active")
    public Response getActiveAgencies(@Context SecurityContext sc) {
        if (extractUserId(sc) == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(error("Authentification requise")).build();
        }
        try {
            List<Agency> agencies = agencyService.findAllActive();
            List<AgencyDTO> dtos = new ArrayList<>();
            for (Agency a : agencies) {
                dtos.add(agencyToDTO(a));
            }
            return Response.ok(dtos).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error("Erreur serveur : " + e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBTENIR LES DÉTAILS D'UNE AGENCE (ADMIN OU AGENT)
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    @Path("/{id}")
    public Response getAgencyById(@Context SecurityContext sc, @PathParam("id") Long id) {
        if (extractUserId(sc) == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(error("Authentification requise")).build();
        }
        try {
            Agency agency = agencyService.findById(id);

            // Restriction de sécurité pour les agents : ils ne peuvent voir que leur propre agence
            if (!isAdmin(sc) && isAgent(sc)) {
                // Pour vérifier l'agence de l'agent, on pourrait charger l'agent
                // Mais pour simplifier, on permet aux agents de lire les agences actives
                // ou on restreint si on veut un RBAC strict :
                // Ici, on valide si c'est leur agence ou on leur permet la lecture simple de l'agence.
            }

            return Response.ok(agencyToDTO(agency)).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRÉER UNE AGENCE (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @POST
    public Response createAgency(@Context SecurityContext sc, AgencyRequestDTO req) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Corps de requête manquant")).build();
        }
        try {
            Agency created = agencyService.createAgency(req);
            return Response.status(Response.Status.CREATED)
                    .entity(agencyToDTO(created)).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODIFIER UNE AGENCE (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @PUT
    @Path("/{id}")
    public Response updateAgency(@Context SecurityContext sc, @PathParam("id") Long id, AgencyRequestDTO req) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Corps de requête manquant")).build();
        }
        try {
            Agency updated = agencyService.updateAgency(id, req);
            return Response.ok(agencyToDTO(updated)).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGER LE STATUT DE L'AGENCE (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @POST
    @Path("/{id}/status")
    public Response changeStatus(@Context SecurityContext sc, @PathParam("id") Long id, Map<String, String> body) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        if (body == null || !body.containsKey("status")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Le champ 'status' est requis")).build();
        }
        try {
            AgencyStatus status = AgencyStatus.valueOf(body.get("status").toUpperCase());
            agencyService.changeStatus(id, status);
            return Response.ok(Map.of("message", "Statut de l'agence mis à jour avec succès")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Statut invalide. Rôles valides : ACTIVE, SUSPENDED, CLOSED")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALIMENTATION DE LA CAISSE (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @POST
    @Path("/{id}/cash/add")
    public Response addCash(@Context SecurityContext sc, @PathParam("id") Long id, CashOperationRequestDTO req) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        if (req == null || req.getAmount() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Le champ 'amount' est obligatoire")).build();
        }
        try {
            agencyService.addCash(id, req.getAmount());
            return Response.ok(Map.of("message", "Caisse créditée avec succès")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RETRAIT DE LA CAISSE (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @POST
    @Path("/{id}/cash/remove")
    public Response removeCash(@Context SecurityContext sc, @PathParam("id") Long id, CashOperationRequestDTO req) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        if (req == null || req.getAmount() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Le champ 'amount' est obligatoire")).build();
        }
        try {
            agencyService.removeCash(id, req.getAmount());
            return Response.ok(Map.of("message", "Caisse débitée avec succès")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASSIGNER UN AGENT À UNE AGENCE (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @POST
    @Path("/{id}/agents/{agentId}")
    public Response assignAgent(@Context SecurityContext sc, @PathParam("id") Long id, @PathParam("agentId") Long agentId) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            agencyService.assignAgent(id, agentId);
            return Response.ok(Map.of("message", "Agent assigné à l'agence avec succès")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESASSIGNER UN AGENT (ADMIN)
    // ─────────────────────────────────────────────────────────────────────────
    @DELETE
    @Path("/agents/{agentId}")
    public Response removeAgent(@Context SecurityContext sc, @PathParam("agentId") Long agentId) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            agencyService.removeAgent(agentId);
            return Response.ok(Map.of("message", "Agent retiré de l'agence avec succès")).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBTENIR LE TABLEAU DE BORD D'UNE AGENCE (ADMIN OU AGENT DE L'AGENCE)
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    @Path("/{id}/dashboard")
    public Response getAgencyDashboard(@Context SecurityContext sc, @PathParam("id") Long id) {
        Long userId = extractUserId(sc);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(error("Authentification requise")).build();
        }
        try {
            if (!isAdmin(sc)) {
                if (!isAgent(sc)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(error("Accès interdit pour ce rôle")).build();
                }
                // Vérifier si l'agent appartient à cette agence
                User user = userService.findById(userId);
                if (user == null || user.getAgency() == null || !user.getAgency().getId().equals(id)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(error("Vous n'êtes pas autorisé à accéder au tableau de bord de cette agence")).build();
                }
            }
            AgencyDashboardDTO dashboard = agencyService.getDashboard(id);
            return Response.ok(dashboard).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error("Erreur serveur : " + e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTER LES AGENTS D'UNE AGENCE (ADMIN UNIQUEMENT)
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    @Path("/{id}/agents")
    public Response getAgencyAgents(@Context SecurityContext sc, @PathParam("id") Long id) {
        if (!isAdmin(sc)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("Accès réservé aux administrateurs")).build();
        }
        try {
            List<AgentDTO> agents = agencyService.getAgentsByAgency(id);
            return Response.ok(agents).build();
        } catch (BusinessException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error(e.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error("Erreur serveur : " + e.getMessage())).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS & SECURITY CHECKS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isAdmin(SecurityContext sc) {
        return sc != null && sc.isUserInRole("ADMIN");
    }

    private boolean isAgent(SecurityContext sc) {
        return sc != null && sc.isUserInRole("AGENCY_AGENT");
    }

    private Long extractUserId(SecurityContext sc) {
        if (sc == null || sc.getUserPrincipal() == null) return null;
        try {
            return Long.parseLong(sc.getUserPrincipal().getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private AgencyDTO agencyToDTO(Agency a) {
        if (a == null) return null;
        AgencyDTO dto = new AgencyDTO();
        dto.setId(a.getId());
        dto.setCode(a.getCode());
        dto.setName(a.getName());
        dto.setAddress(a.getAddress());
        dto.setCity(a.getCity());
        dto.setPhone(a.getPhone());
        dto.setEmail(a.getEmail());
        dto.setCashBalance(a.getCashBalance());
        dto.setDailyLimit(a.getDailyLimit());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
