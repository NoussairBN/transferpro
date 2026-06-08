package ma.transfert.dto;

import ma.transfert.model.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferDTO {
    private Long id;
    private String trackingCode;
    private BigDecimal amount;
    private BigDecimal fees;
    private TransferStatus status;
    private String senderName;
    private String receiverName;
    private String receiverPhone;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private Long sendingAgencyId;
    private String sendingAgencyName;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    
    public Long getSendingAgencyId() { return sendingAgencyId; }
    public void setSendingAgencyId(Long sendingAgencyId) { this.sendingAgencyId = sendingAgencyId; }
    
    public String getSendingAgencyName() { return sendingAgencyName; }
    public void setSendingAgencyName(String sendingAgencyName) { this.sendingAgencyName = sendingAgencyName; }
    
    public BigDecimal getTotalAmount() {
        if (fees != null) {
            return amount.add(fees);
        }
        return amount;
    }
}