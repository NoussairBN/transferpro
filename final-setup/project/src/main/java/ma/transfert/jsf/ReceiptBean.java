package ma.transfert.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import ma.transfert.dto.TransferDTO;
import ma.transfert.service.TransferService;
import ma.transfert.service.doc.PdfReceiptService;

@Named
@ViewScoped
public class ReceiptBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private PdfReceiptService pdfService;
    
    @Inject
    private TransferService transferService;

    private List<TransferDTO> userTransfers;
    private TransferDTO selectedTransfer;
    private Long currentUserId;

    @PostConstruct
    public void init() {
        loadCurrentUserId();
        loadUserTransfers();
    }
    
    private void loadCurrentUserId() {
        try {
            // Try to get user ID from session
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null) {
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                Object userIdObj = request.getSession().getAttribute("userId");
                
                if (userIdObj instanceof Long) {
                    currentUserId = (Long) userIdObj;
                } else if (userIdObj instanceof String) {
                    currentUserId = Long.parseLong((String) userIdObj);
                } else {
                    // Fallback: try to get from a different session attribute
                    userIdObj = context.getExternalContext().getSessionMap().get("currentUserId");
                    if (userIdObj instanceof Long) {
                        currentUserId = (Long) userIdObj;
                    } else {
                        // If no user ID, try to get transfers for demo (agency 1)
                        currentUserId = null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            currentUserId = null;
        }
    }
    
    private void loadUserTransfers() {
        try {
            if (currentUserId != null) {
                // Get transfers for specific user (page 0, size 100)
                // Note: getTransfersByUser expects offset and limit
                // We need to add a method in TransferService or use existing
                userTransfers = transferService.getTransfersByAgency(1L, 0, 100);
                // If you have a method to get by user, use that instead
            } else {
                // Fallback: show transfers from agency 1 (first agency)
                userTransfers = transferService.getTransfersByAgency(1L, 0, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
            userTransfers = List.of(); // Empty list if error
        }
    }

    public void downloadReceipt(TransferDTO transfer) {
        try {
            // Get the full transfer entity using tracking code
            TransferDTO fullTransfer = transferService.getTransferByTrackingCode(transfer.getTrackingCode());
            
            // Generate PDF for the transfer
            Path pdfPath = pdfService.generateReceiptByTrackingCode(fullTransfer.getTrackingCode());
            
            FacesContext facesContext = FacesContext.getCurrentInstance();
            var response = facesContext.getExternalContext();
            
            response.responseReset();
            response.setResponseContentType("application/pdf");
            
            long fileSize = Files.size(pdfPath);
            response.setResponseContentLength((int) fileSize);
            
            response.setResponseHeader("Content-Disposition", 
                "attachment;filename=\"Recu_" + transfer.getTrackingCode() + ".pdf\"");
            
            OutputStream os = response.getResponseOutputStream();
            Files.copy(pdfPath, os);
            
            os.flush();
            facesContext.responseComplete();
            
        } catch (Exception e) {
            e.printStackTrace();
            // Add faces message for error
            FacesContext.getCurrentInstance().addMessage(null, 
                new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR, 
                    "Erreur", 
                    "Impossible de générer le reçu: " + e.getMessage()));
        }
    }
    
    // Helper method to get status badge style
    public String getStatusBadgeStyle(TransferDTO transfer) {
        if (transfer == null || transfer.getStatus() == null) return "badge-warning";
        
        switch (transfer.getStatus()) {
            case PAID:
                return "badge-success";
            case AVAILABLE:
                return "badge-info";
            case EXPIRED:
                return "badge-danger";
            case CANCELLED:
                return "badge-danger";
            case CONFIRMED:
                return "badge-info";
            default:
                return "badge-warning";
        }
    }
    
    public String getStatusLabel(TransferDTO transfer) {
        if (transfer == null || transfer.getStatus() == null) return "INCONNU";
        
        switch (transfer.getStatus()) {
            case PAID:
                return "PAYÉ";
            case AVAILABLE:
                return "DISPONIBLE";
            case EXPIRED:
                return "EXPIRÉ";
            case CANCELLED:
                return "ANNULÉ";
            case CONFIRMED:
                return "CONFIRMÉ";
            case PENDING:
                return "EN ATTENTE";
            default:
                return transfer.getStatus().name();
        }
    }

    // Getters / Setters
    public List<TransferDTO> getUserTransfers() { 
        return userTransfers; 
    }
    
    public TransferDTO getSelectedTransfer() { 
        return selectedTransfer; 
    }
    
    public void setSelectedTransfer(TransferDTO selectedTransfer) { 
        this.selectedTransfer = selectedTransfer; 
    }
    
    public Long getCurrentUserId() {
        return currentUserId;
    }
    
    public void setCurrentUserId(Long currentUserId) {
        this.currentUserId = currentUserId;
    }
}