package ma.transfert.dto;

import ma.transfert.model.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferStatusDTO {
    private String trackingCode;
    private TransferStatus status;
    private BigDecimal amount;
    private BigDecimal fees;
    private String senderName;
    private String receiverName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    // Getters and Setters
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public BigDecimal getTotalAmount() {
        if (fees != null) {
            return amount.add(fees);
        }
        return amount;
    }
}