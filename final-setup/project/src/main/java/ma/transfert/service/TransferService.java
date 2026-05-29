package ma.transfert.service;

import ma.transfert.dao.TransferDAO;
import ma.transfert.dto.TransferCreateDTO;
import ma.transfert.dto.TransferDTO;
import ma.transfert.dto.TransferStatusDTO;
import ma.transfert.model.Agency;
import ma.transfert.model.Transfer;
import ma.transfert.model.User;
import ma.transfert.model.enums.TransferStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Stateless
public class TransferService {
    
    @EJB
    private TransferDAO transferDAO;
    
    @EJB
    private OTPService otpService;
    
    @EJB
    private FeeCalculatorService feeCalculator;
    
    private static final DateTimeFormatter TRACKING_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Transactional
    public TransferDTO createTransfer(TransferCreateDTO createDTO, Agency sendingAgency, User user) {
        // Validate amount
        feeCalculator.validateAmount(createDTO.getAmount());
        
        // Calculate fees
        BigDecimal fees = feeCalculator.calculateFees(createDTO.getAmount());
        
        // Generate tracking code
        String trackingCode = generateTrackingCode();
        
        // Generate OTP
        String otp = otpService.generateOTP();
        
        // Create transfer
        Transfer transfer = new Transfer();
        transfer.setAmount(createDTO.getAmount());
        transfer.setFees(fees);
        transfer.setTrackingCode(trackingCode);
        transfer.setOtpCode(otp);
        transfer.setSenderName(createDTO.getSenderName());
        transfer.setSenderPhone(createDTO.getSenderPhone());
        transfer.setReceiverName(createDTO.getReceiverName());
        transfer.setReceiverPhone(createDTO.getReceiverPhone());
        transfer.setReceiverEmail(createDTO.getReceiverEmail());
        transfer.setSendingAgency(sendingAgency);
        transfer.setUser(user);
        transfer.setNotes(createDTO.getNotes());
        transfer.setStatus(TransferStatus.PENDING);
        
        Transfer saved = transferDAO.save(transfer);
        
        return convertToDTO(saved);
    }
    
    @Transactional
    public TransferDTO confirmTransfer(String trackingCode) {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new RuntimeException("Transfert non trouvé");
        }
        
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Le transfert ne peut pas être confirmé");
        }
        
        transfer.setStatus(TransferStatus.CONFIRMED);
        Transfer updated = transferDAO.update(transfer);
        
        return convertToDTO(updated);
    }
    
    @Transactional
    public TransferDTO makeAvailable(String trackingCode) {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new RuntimeException("Transfert non trouvé");
        }
        
        if (transfer.getStatus() != TransferStatus.CONFIRMED) {
            throw new RuntimeException("Le transfert ne peut être mis à disposition");
        }
        
        transfer.setStatus(TransferStatus.AVAILABLE);
        Transfer updated = transferDAO.update(transfer);
        
        return convertToDTO(updated);
    }
    
    @Transactional
    public TransferDTO payTransfer(String trackingCode, String otp, Agency receivingAgency) {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new RuntimeException("Transfert non trouvé");
        }
        
        if (transfer.getStatus() != TransferStatus.AVAILABLE) {
            throw new RuntimeException("Ce transfert n'est pas disponible pour le paiement");
        }
        
        if (!otp.equals(transfer.getOtpCode())) {
            throw new RuntimeException("Code OTP invalide");
        }
        
        if (transfer.isExpired()) {
            throw new RuntimeException("Ce transfert a expiré");
        }
        
        transfer.markAsPaid();
        transfer.setReceivingAgency(receivingAgency);
        Transfer updated = transferDAO.update(transfer);
        
        return convertToDTO(updated);
    }
    
    @Transactional
    public TransferDTO cancelTransfer(String trackingCode) {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new RuntimeException("Transfert non trouvé");
        }
        
        if (!transfer.getStatus().canBeCancelled()) {
            throw new RuntimeException("Ce transfert ne peut pas être annulé");
        }
        
        transfer.setStatus(TransferStatus.CANCELLED);
        Transfer updated = transferDAO.update(transfer);
        
        return convertToDTO(updated);
    }
    
    public TransferDTO getTransferByTrackingCode(String trackingCode) {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new RuntimeException("Transfert non trouvé");
        }
        return convertToDTO(transfer);
    }
    
    public List<TransferDTO> getTransfersByAgency(Long agencyId, int page, int size) {
        int offset = page * size;
        List<Transfer> transfers = transferDAO.findByAgency(agencyId, offset, size);
        return transfers.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public TransferStatusDTO getTransferStatus(String trackingCode) {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new RuntimeException("Transfert non trouvé");
        }
        
        TransferStatusDTO dto = new TransferStatusDTO();
        dto.setTrackingCode(transfer.getTrackingCode());
        dto.setStatus(transfer.getStatus());
        dto.setAmount(transfer.getAmount());
        dto.setFees(transfer.getFees());
        dto.setSenderName(transfer.getSenderName());
        dto.setReceiverName(transfer.getReceiverName());
        dto.setCreatedAt(transfer.getCreatedAt());
        dto.setExpiresAt(transfer.getExpiresAt());
        
        return dto;
    }
    
    private String generateTrackingCode() {
        String datePart = LocalDateTime.now().format(TRACKING_FORMATTER);
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "TRF-" + datePart + "-" + uniquePart;
    }
    
    private TransferDTO convertToDTO(Transfer transfer) {
        TransferDTO dto = new TransferDTO();
        dto.setId(transfer.getId());
        dto.setTrackingCode(transfer.getTrackingCode());
        dto.setAmount(transfer.getAmount());
        dto.setFees(transfer.getFees());
        dto.setStatus(transfer.getStatus());
        dto.setSenderName(transfer.getSenderName());
        dto.setReceiverName(transfer.getReceiverName());
        dto.setReceiverPhone(transfer.getReceiverPhone());
        dto.setCreatedAt(transfer.getCreatedAt());
        dto.setExpiresAt(transfer.getExpiresAt());
        dto.setPaidAt(transfer.getPaidAt());
        
        if (transfer.getSendingAgency() != null) {
            dto.setSendingAgencyId(transfer.getSendingAgency().getId());
            dto.setSendingAgencyName(transfer.getSendingAgency().getName());
        }
        
        return dto;
    }
}