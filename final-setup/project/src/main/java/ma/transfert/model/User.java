package ma.transfert.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "phone"),
    @UniqueConstraint(columnNames = "cin")
})
@NamedQueries({
    @NamedQuery(name = "User.findByEmail",
        query = "SELECT u FROM User u WHERE u.email = :email"),
    @NamedQuery(name = "User.findByPhone",
        query = "SELECT u FROM User u WHERE u.phone = :phone"),
    @NamedQuery(name = "User.findAllActive",
        query = "SELECT u FROM User u WHERE u.status = 'ACTIVE'")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(unique = true, length = 20)
    private String cin;                      // Carte d'identité nationale

    @Column(nullable = false)
    private String passwordHash;             // BCrypt

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;                   // INDIVIDUAL, AGENCY_AGENT, ADMIN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;                   // Non null si agent d'agence

    // ── Enums internes ──────────────────────────────────────────
    public enum UserRole { INDIVIDUAL, AGENCY_AGENT, ADMIN }
    public enum KycStatus { PENDING, VERIFIED, REJECTED }
    public enum AccountStatus { ACTIVE, SUSPENDED, BLOCKED }

    // ── Getters & Setters ───────────────────────────────────────
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Agency getAgency() { return agency; }
    public void setAgency(Agency agency) { this.agency = agency; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
