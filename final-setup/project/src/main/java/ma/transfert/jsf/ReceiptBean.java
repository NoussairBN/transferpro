package ma.transfert.jsf;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import ma.transfert.dao.TransferDAO;
import ma.transfert.dto.TransferDTO;
import ma.transfert.model.Transfer;
import ma.transfert.service.TransferService;
import ma.transfert.service.doc.PdfReceiptService;

import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Named("receiptBean")
@ViewScoped
public class ReceiptBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @EJB
    private TransferDAO transferDAO;

    @jakarta.inject.Inject
    private PdfReceiptService pdfService;

    @jakarta.inject.Inject
    private TransferService transferService;

    private List<TransferDTO> userTransfers;
    private TransferDTO selectedTransfer;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Long userId = (Long) ctx.getExternalContext().getSessionMap().get("userId");
        String role  = (String) ctx.getExternalContext().getSessionMap().get("userRole");

        if (userId == null) {
            try { ctx.getExternalContext().redirect("login.xhtml"); } catch (Exception e) { e.printStackTrace(); }
            return;
        }

        try {
            List<Transfer> transfers;
            if ("ADMIN".equals(role)) {
                transfers = transferDAO.findAll(0, 100);
            } else {
                transfers = transferDAO.findByUser(userId, 0, 100);
            }
            userTransfers = new ArrayList<>();
            for (Transfer t : transfers) {
                userTransfers.add(toDTO(t));
            }
        } catch (Exception e) {
            e.printStackTrace();
            userTransfers = List.of();
        }
    }

    private TransferDTO toDTO(Transfer t) {
        TransferDTO dto = new TransferDTO();
        dto.setTrackingCode(t.getTrackingCode());
        dto.setAmount(t.getAmount());
        dto.setFees(t.getFees());
        dto.setStatus(t.getStatus());
        dto.setSenderName(t.getSenderName());
        dto.setReceiverName(t.getReceiverName());
        dto.setReceiverPhone(t.getReceiverPhone());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setExpiresAt(t.getExpiresAt());
        dto.setPaidAt(t.getPaidAt());
        if (t.getSendingAgency() != null) dto.setSendingAgencyName(t.getSendingAgency().getName());
        return dto;
    }

    public void downloadReceipt(TransferDTO transfer) {
        try {
            Path pdfPath = pdfService.generateReceiptByTrackingCode(transfer.getTrackingCode());
            FacesContext ctx = FacesContext.getCurrentInstance();
            var ext = ctx.getExternalContext();
            ext.responseReset();
            ext.setResponseContentType("application/pdf");
            ext.setResponseContentLength((int) Files.size(pdfPath));
            ext.setResponseHeader("Content-Disposition", "attachment;filename=\"Recu_" + transfer.getTrackingCode() + ".pdf\"");
            OutputStream os = ext.getResponseOutputStream();
            Files.copy(pdfPath, os);
            os.flush();
            ctx.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Erreur PDF", e.getMessage()));
        }
    }

    public String formatDate(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(FMT) : "-";
    }

    public String getStatusBadge(TransferDTO t) {
        if (t == null || t.getStatus() == null) return "badge-warning";
        return switch (t.getStatus()) {
            case PAID      -> "badge-success";
            case AVAILABLE, CONFIRMED -> "badge-info";
            case EXPIRED, CANCELLED   -> "badge-danger";
            default        -> "badge-warning";
        };
    }

    public String getStatusLabel(TransferDTO t) {
        if (t == null || t.getStatus() == null) return "INCONNU";
        return switch (t.getStatus()) {
            case PAID      -> "PAYÉ";
            case AVAILABLE -> "DISPONIBLE";
            case CONFIRMED -> "CONFIRMÉ";
            case PENDING   -> "EN ATTENTE";
            case EXPIRED   -> "EXPIRÉ";
            case CANCELLED -> "ANNULÉ";
        };
    }

    public int getPaidCount() {
        if (userTransfers == null) return 0;
        return (int) userTransfers.stream().filter(t -> t.getStatus() != null && t.getStatus().name().equals("PAID")).count();
    }
    public int getPendingCount() {
        if (userTransfers == null) return 0;
        return (int) userTransfers.stream().filter(t -> t.getStatus() != null && !t.getStatus().name().equals("PAID") && !t.getStatus().name().equals("CANCELLED") && !t.getStatus().name().equals("EXPIRED")).count();
    }

    public List<TransferDTO> getUserTransfers()  { return userTransfers; }
    public TransferDTO getSelectedTransfer()      { return selectedTransfer; }
    public void setSelectedTransfer(TransferDTO t){ this.selectedTransfer = t; }
}
