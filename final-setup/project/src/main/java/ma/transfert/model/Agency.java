package ma.transfert.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "agencies")
@NamedQueries({
    @NamedQuery(name = "Agency.findByCode",
        query = "SELECT a FROM Agency a WHERE a.code = :code"),
    @NamedQuery(name = "Agency.findAllActive",
        query = "SELECT a FROM Agency a WHERE a.status = 'ACTIVE' ORDER BY a.city")
})
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;                     // Ex: AGC-CASA-001

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cashBalance = BigDecimal.ZERO;  // Solde caisse

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit = new BigDecimal("500000.00");  // Limite journalière MAD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgencyStatus status = AgencyStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "agency", fetch = FetchType.LAZY)
    private List<User> agents;

    @OneToMany(mappedBy = "senderAgency", fetch = FetchType.LAZY)
    private List<Transfer> sentTransfers;

    @OneToMany(mappedBy = "receiverAgency", fetch = FetchType.LAZY)
    private List<Transfer> receivedTransfers;

    public enum AgencyStatus { ACTIVE, SUSPENDED, CLOSED }

    // ── Getters & Setters ───────────────────────────────────────
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal cashBalance) { this.cashBalance = cashBalance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public AgencyStatus getStatus() { return status; }
    public void setStatus(AgencyStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<User> getAgents() { return agents; }
}
