package ma.transfert.rest;

import jakarta.ws.rs.Path;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import ma.transfert.model.Document;
import ma.transfert.model.DocumentType;
import ma.transfert.service.doc.DocumentService;
import ma.transfert.service.doc.PdfReceiptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    private DocumentService documentService;

    @Inject
    private PdfReceiptService pdfReceiptService;

    // ──────────────────────────────────────────────
    // UPLOAD KYC
    // ──────────────────────────────────────────────

    /**
     * POST /api/documents/upload?type=CNI_RECTO
     */
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDocument(
            @Context HttpServletRequest request,
            @QueryParam("type") String documentType,
            @Context SecurityContext securityContext) throws Exception {

        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Authentification requise")).build();
        }
        Long userId = Long.parseLong(securityContext.getUserPrincipal().getName());

        try {
            DocumentType type = DocumentType.valueOf(documentType.toUpperCase());

            Part part = request.getPart("file");
            if (part == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Aucun fichier reçu")).build();
            }

            String contentType = part.getContentType();
            InputStream stream = part.getInputStream();

            String fileName = part.getSubmittedFileName();
            if (fileName == null || fileName.isBlank()) {
                fileName = "document";
            }

            Document saved = documentService.uploadDocument(
                    stream, fileName, contentType, type, userId);

            return Response.ok(Map.of(
                    "id", saved.getId(),
                    "originalName", saved.getOriginalName(),
                    "type", saved.getType(),
                    "status", saved.getStatus(),
                    "uploadedAt", saved.getUploadedAt().toString()
            )).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", "Erreur lors de l'upload : " + e.getMessage())).build();
        }
    }

    // ──────────────────────────────────────────────
    // LISTE DES DOCUMENTS (user connecté)
    // ──────────────────────────────────────────────

    /**
     * GET /api/documents/my
     */
    @GET
    @Path("/my")
    public Response getMyDocuments(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Long userId = Long.parseLong(securityContext.getUserPrincipal().getName());
        List<Document> docs = documentService.getDocumentsByUser(userId);
        return Response.ok(docs.stream().map(this::toMap).toList()).build();
    }

    // ──────────────────────────────────────────────
    // ADMIN : VALIDATION KYC
    // ──────────────────────────────────────────────

    /**
     * PUT /api/documents/{id}/validate
     */
    @PUT
    @Path("/{id}/validate")
    public Response validateDocument(
            @PathParam("id") Long id,
            @QueryParam("comment") @DefaultValue("Document validé") String comment,
            @Context SecurityContext securityContext) {

        if (!securityContext.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Accès réservé aux administrateurs")).build();
        }

        try {
            Document doc = documentService.validateDocument(id, comment);
            return Response.ok(toMap(doc)).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * PUT /api/documents/{id}/reject
     */
    @PUT
    @Path("/{id}/reject")
    public Response rejectDocument(
            @PathParam("id") Long id,
            @QueryParam("reason") @DefaultValue("Document non conforme") String reason,
            @Context SecurityContext securityContext) {

        if (!securityContext.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Accès réservé aux administrateurs")).build();
        }

        try {
            Document doc = documentService.rejectDocument(id, reason);
            return Response.ok(toMap(doc)).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/documents/pending
     */
    @GET
    @Path("/pending")
    public Response getPendingDocuments(@Context SecurityContext securityContext) {
        if (!securityContext.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<Document> docs = documentService.getPendingDocuments();
        return Response.ok(docs.stream().map(this::toMap).toList()).build();
    }

    // ──────────────────────────────────────────────
    // REÇUS PDF — intégration DEV-2
    // ──────────────────────────────────────────────

    /**
     * GET /api/documents/receipt/{transferId}
     */
    @GET
    @Path("/receipt/{transferId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadReceipt(
            @PathParam("transferId") Long transferId,
            @Context SecurityContext securityContext) {

        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            // Explicitly using java.nio.file.Path here to avoid conflict
            java.nio.file.Path pdfPath = pdfReceiptService.generateReceiptForTransfer(transferId);
            byte[] pdfBytes = Files.readAllBytes(pdfPath);

            return Response.ok(pdfBytes)
                    .header("Content-Disposition",
                            "attachment; filename=\"receipt-" + transferId + ".pdf\"")
                    .type("application/pdf")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", "Erreur génération PDF : " + e.getMessage())).build();
        }
    }

    /**
     * GET /api/documents/receipt/tracking/{code}
     */
    @GET
    @Path("/receipt/tracking/{code}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadReceiptByCode(
            @PathParam("code") String trackingCode,
            @Context SecurityContext securityContext) {

        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            // Explicitly using java.nio.file.Path here to avoid conflict
            java.nio.file.Path pdfPath = pdfReceiptService.generateReceiptByTrackingCode(trackingCode);
            byte[] pdfBytes = Files.readAllBytes(pdfPath);

            return Response.ok(pdfBytes)
                    .header("Content-Disposition",
                            "attachment; filename=\"receipt-" + trackingCode + ".pdf\"")
                    .type("application/pdf")
                    .build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────
    private Map<String, Object> toMap(Document doc) {
        return Map.of(
                "id", doc.getId(),
                "originalName", doc.getOriginalName(),
                "type", doc.getType().name(),
                "status", doc.getStatus().name(),
                "uploadedAt", doc.getUploadedAt().toString(),
                "adminComment", doc.getAdminComment() != null ? doc.getAdminComment() : ""
        );
    }
}