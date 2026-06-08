package ma.transfert.dto;

import ma.transfert.model.Agency.AgencyStatus;
import java.math.BigDecimal;

public class AgencyDashboardDTO {

    // ── Informations de l'agence ──
    private Long agencyId;
    private String agencyCode;
    private String agencyName;
    private AgencyStatus status;

    // ── KPIs Transferts ──
    private long totalTransfers;
    private long pendingTransfers;
    private long confirmedTransfers;
    private long paidTransfers;
    private long cancelledTransfers;

    // ── Volumes financiers ──
    private BigDecimal totalVolumeSent;
    private BigDecimal totalVolumeReceived;
    private BigDecimal totalFeesCollected;

    // ── Caisse ──
    private BigDecimal cashBalance;
    private BigDecimal dailyLimit;

    // ── Agents ──
    private int agentCount;

    // Constructeur par défaut
    public AgencyDashboardDTO() {}

    // ── Getters & Setters ──
    public Long getAgencyId() { return agencyId; }
    public void setAgencyId(Long agencyId) { this.agencyId = agencyId; }

    public String getAgencyCode() { return agencyCode; }
    public void setAgencyCode(String agencyCode) { this.agencyCode = agencyCode; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public AgencyStatus getStatus() { return status; }
    public void setStatus(AgencyStatus status) { this.status = status; }

    public long getTotalTransfers() { return totalTransfers; }
    public void setTotalTransfers(long totalTransfers) { this.totalTransfers = totalTransfers; }

    public long getPendingTransfers() { return pendingTransfers; }
    public void setPendingTransfers(long pendingTransfers) { this.pendingTransfers = pendingTransfers; }

    public long getConfirmedTransfers() { return confirmedTransfers; }
    public void setConfirmedTransfers(long confirmedTransfers) { this.confirmedTransfers = confirmedTransfers; }

    public long getPaidTransfers() { return paidTransfers; }
    public void setPaidTransfers(long paidTransfers) { this.paidTransfers = paidTransfers; }

    public long getCancelledTransfers() { return cancelledTransfers; }
    public void setCancelledTransfers(long cancelledTransfers) { this.cancelledTransfers = cancelledTransfers; }

    public BigDecimal getTotalVolumeSent() { return totalVolumeSent; }
    public void setTotalVolumeSent(BigDecimal totalVolumeSent) { this.totalVolumeSent = totalVolumeSent; }

    public BigDecimal getTotalVolumeReceived() { return totalVolumeReceived; }
    public void setTotalVolumeReceived(BigDecimal totalVolumeReceived) { this.totalVolumeReceived = totalVolumeReceived; }

    public BigDecimal getTotalFeesCollected() { return totalFeesCollected; }
    public void setTotalFeesCollected(BigDecimal totalFeesCollected) { this.totalFeesCollected = totalFeesCollected; }

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }

    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }

    public int getAgentCount() { return agentCount; }
    public void setAgentCount(int agentCount) { this.agentCount = agentCount; }
}
