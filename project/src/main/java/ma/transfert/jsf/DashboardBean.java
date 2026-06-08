package ma.transfert.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import ma.transfert.dao.DocumentDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.model.Document;
import ma.transfert.model.DocumentStatus;
import ma.transfert.model.User;

import java.util.List;

@Named("dashboardBean")
@RequestScoped
public class DashboardBean {

    @EJB
    private DocumentDAO documentDAO;

    @EJB
    private UserDAO userDAO;

    private int documentCount;
    private int validatedCount;
    private int pendingCount;
    private String kycStatus = "PENDING";

    @PostConstruct
    public void init() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    Long userId = (Long) ctx.getExternalContext().getSessionMap().get("userId");

    // Sécurité : Si aucun utilisateur n'est en session, on évite d'interroger la base
    if (userId == null) {
        try {
            // Rediriger vers la page de login si non connecté
            ctx.getExternalContext().redirect("login.xhtml?faces-redirect=true");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return; 
    }

    try {
        List<Document> docs = documentDAO.findByOwner(userId);
        if (docs != null) {
            documentCount = docs.size();
            validatedCount = (int) docs.stream()
                    .filter(d -> d.getStatus() == DocumentStatus.VALIDATED).count();
            pendingCount = (int) docs.stream()
                    .filter(d -> d.getStatus() == DocumentStatus.PENDING).count();
        }

        User user = userDAO.findById(userId);
        if (user != null && user.getKycStatus() != null) {
            kycStatus = user.getKycStatus().name();
        }
    } catch (Exception e) {
        // Permet de voir précisément dans les logs si un autre problème SQL survient
        System.err.println("Erreur lors du chargement des données du dashboard: " + e.getMessage());
        e.printStackTrace();
    }
}
    public int getDocumentCount() { return documentCount; }
    public int getValidatedCount() { return validatedCount; }
    public int getPendingCount() { return pendingCount; }
    public String getKycStatus() { return kycStatus; }
}