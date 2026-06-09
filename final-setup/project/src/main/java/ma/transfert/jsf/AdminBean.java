package ma.transfert.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ma.transfert.dao.TransferDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.dto.TransferDTO;
import ma.transfert.model.Document;
import ma.transfert.model.Transfer;
import ma.transfert.model.User;
import ma.transfert.service.TransferService;
import ma.transfert.service.doc.DocumentService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Named("adminBean")
@ViewScoped
public class AdminBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @EJB private DocumentService documentService;
    @EJB private UserDAO userDAO;
    @EJB private TransferDAO transferDAO;
    @EJB private TransferService transferService;

    private List<Document> pendingDocuments;
    private List<User> allUsers;
    private List<TransferDTO> recentTransfers;

    private String rejectReason = "";
    private String activeTab = "kyc";

    // Stats
    private int totalUsers;
    private int pendingKycCount;
    private int totalTransfersCount;
    private BigDecimal totalVolume = BigDecimal.ZERO;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        String role = (String) ctx.getExternalContext().getSessionMap().get("userRole");
        if (!"ADMIN".equals(role)) {
            try { ctx.getExternalContext().redirect("user-dashboard.xhtml"); } catch (Exception e) { e.printStackTrace(); }
            return;
        }
        loadData();
    }

    private void loadData() {
        try { pendingDocuments = documentService.getPendingDocuments(); } catch (Exception e) { pendingDocuments = List.of(); }
        try { allUsers = userDAO.findAllActive(); } catch (Exception e) { allUsers = List.of(); }
        try { recentTransfers = transferService.getAllTransfers(0, 50); } catch (Exception e) { recentTransfers = List.of(); }

        totalUsers        = allUsers != null ? allUsers.size() : 0;
        pendingKycCount   = pendingDocuments != null ? pendingDocuments.size() : 0;
        totalTransfersCount = recentTransfers != null ? recentTransfers.size() : 0;
        totalVolume = recentTransfers != null ? recentTransfers.stream()
            .map(TransferDTO::getAmount).filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
    }

    public void validateDocument(Long documentId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            documentService.validateDocument(documentId, "Document validé par l'administrateur");
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "✓ Document validé avec succès", null));
            loadData();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
    }

    public void rejectDocument(Long documentId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            String reason = (rejectReason != null && !rejectReason.isBlank()) ? rejectReason : "Document non conforme";
            documentService.rejectDocument(documentId, reason);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Document rejeté", null));
            rejectReason = "";
            loadData();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
    }

    public void confirmTransfer(String trackingCode) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            transferService.confirmTransfer(trackingCode);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Transfert confirmé", null));
            loadData();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
    }

    public void makeAvailable(String trackingCode) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            transferService.makeAvailable(trackingCode);
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Transfert disponible pour retrait", null));
            loadData();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
    }

    public void suspendUser(Long userId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            User user = userDAO.findById(userId);
            if (user != null) { user.setStatus(User.AccountStatus.SUSPENDED); userDAO.update(user); }
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Utilisateur suspendu", null));
            loadData();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
    }

    public void activateUser(Long userId) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            User user = userDAO.findById(userId);
            if (user != null) { user.setStatus(User.AccountStatus.ACTIVE); userDAO.update(user); }
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Utilisateur réactivé", null));
            loadData();
        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
    }

    public String formatDate(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(FMT) : "-";
    }

    public String getTransferStatusClass(TransferDTO t) {
        if (t == null || t.getStatus() == null) return "badge-pending";
        return switch (t.getStatus()) {
            case PAID      -> "badge-paid";
            case AVAILABLE, CONFIRMED -> "badge-available";
            case CANCELLED, EXPIRED   -> "badge-cancelled";
            default        -> "badge-pending";
        };
    }

    public String getTransferStatusLabel(TransferDTO t) {
        if (t == null || t.getStatus() == null) return "-";
        return switch (t.getStatus()) {
            case PAID      -> "Payé";
            case AVAILABLE -> "Disponible";
            case CONFIRMED -> "Confirmé";
            case PENDING   -> "En attente";
            case EXPIRED   -> "Expiré";
            case CANCELLED -> "Annulé";
        };
    }

    // Getters / Setters
    public List<Document>    getPendingDocuments()  { return pendingDocuments; }
    public List<User>        getAllUsers()           { return allUsers; }
    public List<TransferDTO> getRecentTransfers()   { return recentTransfers; }
    public int  getPendingKycCount()    { return pendingKycCount; }
    public int  getTotalUsers()         { return totalUsers; }
    public int  getTotalTransfersCount(){ return totalTransfersCount; }
    public BigDecimal getTotalVolume()  { return totalVolume; }
    public String getRejectReason()     { return rejectReason; }
    public void setRejectReason(String v){ this.rejectReason = v; }
    public String getActiveTab()        { return activeTab; }
    public void setActiveTab(String v)  { this.activeTab = v; }
}
