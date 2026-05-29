package ma.transfert.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.transfert.service.doc.PdfReceiptService;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    private PdfReceiptService pdfReceiptService;

    @GET
    @Path("/test-receipt")
    public Response testReceipt() {
        try {
            return Response.ok(pdfReceiptService.generateFakeReceipt().toString()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
