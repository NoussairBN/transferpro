package ma.transfert.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.transfert.model.Document;
import ma.transfert.model.DocumentType;
import ma.transfert.service.doc.DocumentService;
import org.primefaces.model.file.UploadedFile;

import java.io.Serializable;
import java.util.List;

/**
 * Managed Bean JSF pour la page kyc-upload.xhtml.
 * ✅ Utilise DocumentService (DEV-5) + l'userId du HttpSession (mis par DEV-3 login).
 */
@Named("kycBean")
@ViewScoped
public class KycBean implements Serializable {

    @Inject
    private DocumentService documentService;

    private UploadedFile uploadedFile;
    private String selectedType;
    private List<Document> myDocuments;

    @PostConstruct
    public void init() {
        loadMyDocuments();
    }

    public String uploadDocument() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (uploadedFile == null || uploadedFile.getContent().length == 0) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Aucun fichier sélectionné", null));
            return null;
        }

        if (selectedType == null || selectedType.isBlank()) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Veuillez sélectionner un type", null));
            return null;
        }

        try {
            // ✅ Récupérer l'userId depuis la session HTTP (mis lors du login DEV-3)
            Long userId = (Long) ctx.getExternalContext()
                    .getSessionMap().get("userId");

            if (userId == null) {
                return "login?faces-redirect=true";
            }

            DocumentType type = DocumentType.valueOf(selectedType);

            documentService.uploadDocument(
                    uploadedFile.getInputStream(),
                    uploadedFile.getFileName(),
                    uploadedFile.getContentType(),
                    type,
                    userId
            );

            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Document uploadé avec succès. En attente de validation.", null));

            loadMyDocuments();

        } catch (Exception e) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Erreur : " + e.getMessage(), null));
        }

        return null;
    }

    private void loadMyDocuments() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Long userId = (Long) ctx.getExternalContext().getSessionMap().get("userId");
        if (userId != null) {
            myDocuments = documentService.getDocumentsByUser(userId);
        }
    }

    // Getters / Setters
    public UploadedFile getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(UploadedFile uploadedFile) { this.uploadedFile = uploadedFile; }

    public String getSelectedType() { return selectedType; }
    public void setSelectedType(String selectedType) { this.selectedType = selectedType; }

    public List<Document> getMyDocuments() { return myDocuments; }
}