package ma.transfert.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ma.transfert.model.enums.TransferStatus;

@Entity
@Table(name = "transfers")
@NamedQueries({
    @NamedQuery(name = "Transfer.findByTrackingCode", 
                query = "SELECT t FROM Transfer t WHERE t.trackingCode = :code"),
    @NamedQuery(name = "Transfer.findByStatus", 
                query = "SELECT t FROM Transfer t WHERE t.status = :status ORDER BY t.createdAt DESC"),
    @NamedQuery(name = "Transfer.findByAgency", 
                query = "SELECT t FROM Transfer t WHERE t.sendingAgency.id = :agencyId ORDER BY t.createdAt DESC"),
    @NamedQuery(name = "Transfer.findByOTPCode", 
                query = "SELECT t FROM Transfer t WHERE t.otpCode = :otpCode AND t.status = :status")
})
public class Transfer implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tracking_code", unique = true, nullable = false, length = 50)
    private String trackingCode;
    
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "fees", precision = 10, scale = 2)
    private BigDecimal fees;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;
    
    @Column(name = "otp_code", length = 8)
    private String otpCode;
    
    @ManyToOne
    @JoinColumn(name = "sending_agency_id")
    private Agency sendingAgency;
    
    @ManyToOne
    @JoinColumn(name = "receiving_agency_id")
    private Agency receivingAgency;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;
    
    @Column(name = "sender_phone", length = 20)
    private String senderPhone;
    
    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;
    
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;
    
    @Column(name = "receiver_email", length = 100)
    private String receiverEmail;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    @Version
    @Column(name = "version")
    private Integer version;
    
    public Transfer() {
        this.createdAt = LocalDateTime.now();
        this.status = TransferStatus.PENDING;
        this.expiresAt = this.createdAt.plusDays(30);
        this.fees = BigDecimal.ZERO;
    }
    
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
    
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    
    public Agency getSendingAgency() { return sendingAgency; }
    public void setSendingAgency(Agency sendingAgency) { this.sendingAgency = sendingAgency; }
    
    public Agency getReceivingAgency() { return receivingAgency; }
    public void setReceivingAgency(Agency receivingAgency) { this.receivingAgency = receivingAgency; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    
    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    // Business methods
    public BigDecimal getTotalAmount() {
        return amount.add(fees != null ? fees : BigDecimal.ZERO);
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void markAsPaid() {
        this.status = TransferStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }
    
    public void markAsAvailable() {
        this.status = TransferStatus.AVAILABLE;
    }
}