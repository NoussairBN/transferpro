package ma.transfert.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", indexes = {
    @Index(name = "idx_transfer_code", columnList = "trackingCode"),
    @Index(name = "idx_transfer_sender", columnList = "sender_id"),
    @Index(name = "idx_transfer_status", columnList = "status")
})
@NamedQueries({
    @NamedQuery(name = "Transfer.findByCode",
        query = "SELECT t FROM Transfer t WHERE t.trackingCode = :code"),
    @NamedQuery(name = "Transfer.findBySender",
        query = "SELECT t FROM Transfer t WHERE t.sender.id = :senderId ORDER BY t.createdAt DESC"),
    @NamedQuery(name = "Transfer.findPendingByAgency",
        query = "SELECT t FROM Transfer t WHERE t.receiverAgency.id = :agencyId AND t.status = 'PENDING'")
})
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String trackingCode;             // Ex: TRF-2024-000001

    // ── Expéditeur ───────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 20)
    private String senderPhone;

    // ── Bénéficiaire ─────────────────────────────────────────────
    @Column(nullable = false, length = 100)
    private String receiverName;

    @Column(nullable = false, length = 20)
    private String receiverPhone;

    @Column(length = 20)
    private String receiverCin;

    // ── Montants ─────────────────────────────────────────────────
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;               // Montant envoyé (MAD)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fees;                 // Frais de transfert

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;          // amount + fees

    @Column(length = 3)
    private String currency = "MAD";

    // ── Agences ──────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_agency_id")
    private Agency senderAgency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_agency_id")
    private Agency receiverAgency;

    // ── Statut & OTP ─────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(length = 6)
    private String otpCode;                  // Code de retrait à 6 chiffres

    @Column
    private LocalDateTime otpExpiresAt;

    // ── Audit ────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime paidAt;

    @Column
    private LocalDateTime cancelledAt;

    @Column(length = 500)
    private String notes;

    // ── Enum ─────────────────────────────────────────────────────
    public enum TransferStatus {
        PENDING,    // Créé, en attente de retrait
        PAID,       // Retiré par le bénéficiaire
        CANCELLED,  // Annulé
        EXPIRED     // OTP expiré
    }

    // ── Logique métier ───────────────────────────────────────────
    public boolean isExpired() {
        return otpExpiresAt != null && LocalDateTime.now().isAfter(otpExpiresAt);
    }

    public boolean validateOtp(String inputOtp) {
        return !isExpired() && otpCode != null && otpCode.equals(inputOtp);
    }

    // ── Getters & Setters ─────────────────────────────────────────
    public Long getId() { return id; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverCin() { return receiverCin; }
    public void setReceiverCin(String receiverCin) { this.receiverCin = receiverCin; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public Agency getSenderAgency() { return senderAgency; }
    public void setSenderAgency(Agency senderAgency) { this.senderAgency = senderAgency; }
    public Agency getReceiverAgency() { return receiverAgency; }
    public void setReceiverAgency(Agency receiverAgency) { this.receiverAgency = receiverAgency; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
