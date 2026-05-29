package ma.transfert.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "cash_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal cashBalance = BigDecimal.ZERO;

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit = new BigDecimal("500000.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AgencyStatus status = AgencyStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "agency", fetch = FetchType.LAZY)
    private List<User> agents;

    @OneToMany(mappedBy = "sendingAgency", fetch = FetchType.LAZY)
    private List<Transfer> sentTransfers;

    @OneToMany(mappedBy = "receivingAgency", fetch = FetchType.LAZY)
    private List<Transfer> receivedTransfers;

    public enum AgencyStatus { ACTIVE, SUSPENDED, CLOSED }

    // Constructeurs
    public Agency() {}

    public Agency(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<User> getAgents() { return agents; }
    public void setAgents(List<User> agents) { this.agents = agents; }

    public List<Transfer> getSentTransfers() { return sentTransfers; }
    public void setSentTransfers(List<Transfer> sentTransfers) { this.sentTransfers = sentTransfers; }

    public List<Transfer> getReceivedTransfers() { return receivedTransfers; }
    public void setReceivedTransfers(List<Transfer> receivedTransfers) { this.receivedTransfers = receivedTransfers; }
}