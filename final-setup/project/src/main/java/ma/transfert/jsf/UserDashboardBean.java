package ma.transfert.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ma.transfert.dao.AgencyDAO;
import ma.transfert.dao.DocumentDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.dto.TransferCreateDTO;
import ma.transfert.dto.TransferDTO;
import ma.transfert.model.Agency;
import ma.transfert.model.Document;
import ma.transfert.model.DocumentStatus;
import ma.transfert.model.User;
import ma.transfert.service.TransferService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Named("userDashboardBean")
@ViewScoped
public class UserDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @EJB private TransferService transferService;
    @EJB private AgencyDAO agencyDAO;
    @EJB private UserDAO userDAO;
    @EJB private DocumentDAO documentDAO;

    // Transfer form fields
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String receiverEmail;
    private BigDecimal amount;
    private String notes;
    private Long selectedAgencyId;

    // State
    private List<Agency> activeAgencies;
    private List<TransferDTO> myTransfers;
    private List<Document> myDocuments;
    private TransferDTO lastCreatedTransfer;
    private String lastOtp;
    private User currentUser;

    // Stats
    private int totalTransfers;
    private int pendingDocs;
    private int validatedDocs;
    private BigDecimal totalSent = BigDecimal.ZERO;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Long userId = (Long) ctx.getExternalContext().getSessionMap().get("userId");
        if (userId == null) {
            try { ctx.getExternalContext().redirect("login.xhtml"); } catch (Exception e) { e.printStackTrace(); }
            return;
        }
        try {
            currentUser = userDAO.findById(userId);
            activeAgencies = agencyDAO.findAllActive();
            if (!activeAgencies.isEmpty()) selectedAgencyId = activeAgencies.get(0).getId();

            // Pre-fill sender from logged-in user
            if (currentUser != null) {
                senderName = currentUser.getFirstName() + " " + currentUser.getLastName();
                senderPhone = currentUser.getPhone();
            }

            myTransfers = transferService.getTransfersByUser(userId, 0, 50);
            myDocuments = documentDAO.findByOwner(userId);

            totalTransfers = myTransfers != null ? myTransfers.size() : 0;
            pendingDocs   = myDocuments != null ? (int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.PENDING).count() : 0;
            validatedDocs = myDocuments != null ? (int) myDocuments.stream().filter(d -> d.getStatus() == DocumentStatus.VALIDATED).count() : 0;
            totalSent = myTransfers != null ? myTransfers.stream()
                .map(TransferDTO::getAmount).filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String createTransfer() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Long userId = (Long) ctx.getExternalContext().getSessionMap().get("userId");
        try {
            if (amount == null || amount.compareTo(BigDecimal.valueOf(50)) < 0) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Montant minimum : 50 MAD", null));
                return null;
            }
            if (receiverPhone == null || receiverPhone.isBlank()) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Téléphone bénéficiaire obligatoire", null));
                return null;
            }

            Agency agency = selectedAgencyId != null ? agencyDAO.findById(selectedAgencyId) : (activeAgencies.isEmpty() ? null : activeAgencies.get(0));
            User user = userDAO.findById(userId);

            TransferCreateDTO dto = new TransferCreateDTO();
            dto.setAmount(amount);
            dto.setSenderName(senderName);
            dto.setSenderPhone(senderPhone);
            dto.setReceiverName(receiverName);
            dto.setReceiverPhone(receiverPhone);
            dto.setReceiverEmail(receiverEmail);
            dto.setNotes(notes);

            TransferDTO result = transferService.createTransfer(dto, agency, user);
            lastCreatedTransfer = result;

            // Get OTP from DB for display
            try {
                var transfer = new ma.transfert.dao.TransferDAO();
            } catch (Exception ignore) {}

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Transfert créé ! Code : " + result.getTrackingCode(), null));

            // Reset form
            receiverName = null; receiverPhone = null; receiverEmail = null;
            amount = null; notes = null;

            // Reload
            myTransfers = transferService.getTransfersByUser(userId, 0, 50);
            totalTransfers = myTransfers.size();

        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erreur : " + e.getMessage(), null));
        }
        return null;
    }

    public String formatDate(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(FMT) : "-";
    }

    public String getStatusBadgeClass(TransferDTO t) {
        if (t == null || t.getStatus() == null) return "badge-pending";
        return switch (t.getStatus()) {
            case PAID      -> "badge-paid";
            case AVAILABLE, CONFIRMED -> "badge-available";
            case CANCELLED, EXPIRED   -> "badge-cancelled";
            default        -> "badge-pending";
        };
    }

    public String getStatusLabel(TransferDTO t) {
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

    public String getKycStatusClass() {
        if (currentUser == null) return "kyc-pending";
        return switch (currentUser.getKycStatus()) {
            case VERIFIED -> "kyc-verified";
            case REJECTED -> "kyc-rejected";
            default       -> "kyc-pending";
        };
    }

    public String getKycLabel() {
        if (currentUser == null) return "Non vérifié";
        return switch (currentUser.getKycStatus()) {
            case VERIFIED -> "Identité vérifiée";
            case REJECTED -> "Documents rejetés";
            default       -> "Vérification en attente";
        };
    }

    // Getters / Setters
    public String getSenderName()   { return senderName; }
    public void setSenderName(String v) { this.senderName = v; }
    public String getSenderPhone()  { return senderPhone; }
    public void setSenderPhone(String v) { this.senderPhone = v; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String v) { this.receiverName = v; }
    public String getReceiverPhone(){ return receiverPhone; }
    public void setReceiverPhone(String v) { this.receiverPhone = v; }
    public String getReceiverEmail(){ return receiverEmail; }
    public void setReceiverEmail(String v) { this.receiverEmail = v; }
    public BigDecimal getAmount()   { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getNotes()        { return notes; }
    public void setNotes(String v)  { this.notes = v; }
    public Long getSelectedAgencyId(){ return selectedAgencyId; }
    public void setSelectedAgencyId(Long v) { this.selectedAgencyId = v; }
    public List<Agency> getActiveAgencies() { return activeAgencies; }
    public List<TransferDTO> getMyTransfers() { return myTransfers; }
    public List<Document> getMyDocuments()    { return myDocuments; }
    public TransferDTO getLastCreatedTransfer(){ return lastCreatedTransfer; }
    public User getCurrentUser()   { return currentUser; }
    public int getTotalTransfers() { return totalTransfers; }
    public int getPendingDocs()    { return pendingDocs; }
    public int getValidatedDocs()  { return validatedDocs; }
    public BigDecimal getTotalSent(){ return totalSent; }
}
