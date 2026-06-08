package ma.transfert.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;

@Path("/health")
@Tag(name = "Health", description = "Vérification de l'état du serveur")
public class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Statut de l'application",
            description = "Retourne l'état du serveur et de la base de données"
    )
    @APIResponse(responseCode = "200", description = "Serveur opérationnel")
    @APIResponse(responseCode = "503", description = "Serveur dégradé")
    public Response health() {
        return Response.ok(Map.of(
                "status", "UP",
                "application", "MoneyTransfer JEE",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now().toString()
        )).build();
    }
}