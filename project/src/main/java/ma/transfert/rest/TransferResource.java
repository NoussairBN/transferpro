package ma.transfert.rest;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.transfert.dao.AgencyDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.dto.TransferCreateDTO;
import ma.transfert.dto.TransferDTO;
import ma.transfert.dto.TransferStatusDTO;
import ma.transfert.model.Agency;
import ma.transfert.model.User;
import ma.transfert.service.TransferService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/transfers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transferts", description = "Gestion des transferts d'argent")

public class TransferResource {

    @EJB
    private TransferService transferService;
    
    @EJB
    private AgencyDAO agencyDAO;
    
    @EJB
    private UserDAO userDAO;
    
    @POST
    @Operation(summary = "Créer un transfert", description = "Crée un nouveau transfert et génère un code OTP")
    @APIResponse(responseCode = "201", description = "Transfert créé avec succès")
    @APIResponse(responseCode = "400", description = "Données invalides")
    public Response createTransfer(TransferCreateDTO createDTO) {
        try {
            // Charger l'agence depuis la base de données (gérée par JPA)
            Agency agency = agencyDAO.findById(1L);
            if (agency == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Agence non trouvée")).build();
            }
            
            // Charger l'utilisateur depuis la base de données
            User user = userDAO.findById(2L);
            if (user == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Utilisateur non trouvé")).build();
            }
            
            TransferDTO result = transferService.createTransfer(createDTO, agency, user);
            return Response.status(Response.Status.CREATED).entity(result).build();
            
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erreur interne: " + e.getMessage())).build();
        }

    }

    
    @POST
    @Path("/{trackingCode}/confirm")
    @Operation(summary = "Confirmer un transfert", description = "Passe le statut de PENDING à CONFIRMED")
    @APIResponse(responseCode = "200", description = "Transfert confirmé")
    public Response confirmTransfer(@PathParam("trackingCode") String trackingCode) {
        try {
            TransferDTO result = transferService.confirmTransfer(trackingCode);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    
    @POST
    @Path("/{trackingCode}/available")
    public Response makeAvailable(@PathParam("trackingCode") String trackingCode) {
        try {
            TransferDTO result = transferService.makeAvailable(trackingCode);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }
    
    @POST
    @Path("/{trackingCode}/pay")
    @Operation(summary = "Payer un transfert", description = "Le bénéficiaire retire l'argent en agence avec son OTP")
    @APIResponse(responseCode = "200", description = "Transfert payé")
    @APIResponse(responseCode = "400", description = "OTP invalide")
    public Response payTransfer(@PathParam("trackingCode") String trackingCode, 
                                 @QueryParam("otp") String otp) {
        try {
            Agency agency = agencyDAO.findById(1L);
            TransferDTO result = transferService.payTransfer(trackingCode, otp, agency);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    
    @POST
    @Path("/{trackingCode}/cancel")
    @Operation(summary = "Annuler un transfert", description = "Annule un transfert en statut PENDING")
    @APIResponse(responseCode = "200", description = "Transfert annulé")
    public Response cancelTransfer(@PathParam("trackingCode") String trackingCode) {
        try {
            TransferDTO result = transferService.cancelTransfer(trackingCode);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/{trackingCode}")
    @Operation(summary = "Détails d'un transfert", description = "Récupère les informations d'un transfert par son code")
    @APIResponse(responseCode = "200", description = "Transfert trouvé")
    @APIResponse(responseCode = "404", description = "Transfert non trouvé")
    public Response getTransfer(@PathParam("trackingCode") String trackingCode) {
        try {
            TransferDTO result = transferService.getTransferByTrackingCode(trackingCode);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }
    @Operation(summary = "Détails d'un transfert", description = "Récupère les informations d'un transfert par son code")
    @APIResponse(responseCode = "200", description = "Transfert trouvé")
    @APIResponse(responseCode = "404", description = "Transfert non trouvé")
    
    @GET
    @Path("/track/{trackingCode}/status")
    public Response trackTransfer(@PathParam("trackingCode") String trackingCode) {
        try {
            TransferStatusDTO result = transferService.getTransferStatus(trackingCode);
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/agency/{agencyId}")
    public Response getAgencyTransfers(@PathParam("agencyId") Long agencyId,
                                        @QueryParam("page") @DefaultValue("0") int page,
                                        @QueryParam("size") @DefaultValue("20") int size) {
        try {
            List<TransferDTO> transfers = transferService.getTransfersByAgency(agencyId, page, size);
            return Response.ok(transfers).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(e.getMessage())).build();
        }
    }
    
    // Error Response Class
    public static class ErrorResponse {
        private final String message;
        private final long timestamp;
        
        public ErrorResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}