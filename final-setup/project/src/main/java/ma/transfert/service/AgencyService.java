package ma.transfert.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import ma.transfert.dao.AgencyDAO;
import ma.transfert.dao.TransferDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.dto.AgencyDashboardDTO;
import ma.transfert.dto.AgencyRequestDTO;
import ma.transfert.dto.AgentDTO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.Agency;
import ma.transfert.model.Agency.AgencyStatus;
import ma.transfert.model.User;
import ma.transfert.model.enums.TransferStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class AgencyService {

    @EJB
    private AgencyDAO agencyDAO;

    @EJB
    private UserDAO userDAO;

    @EJB
    private TransferDAO transferDAO;

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD AGENCES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée une nouvelle agence après validation.
     */
    public Agency createAgency(AgencyRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException("Les données de l'agence sont requises");
        }
        validateAgencyRequest(dto);

        // Unicité du code
        if (agencyDAO.findByCode(dto.getCode().trim()) != null) {
            throw new BusinessException("Une agence avec ce code existe déjà");
        }

        Agency agency = new Agency();
        agency.setCode(dto.getCode().trim().toUpperCase());
        agency.setName(dto.getName().trim());
        agency.setAddress(dto.getAddress().trim());
        agency.setCity(dto.getCity().trim());
        agency.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        agency.setEmail(dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null);
        agency.setDailyLimit(dto.getDailyLimit() != null ? dto.getDailyLimit() : new BigDecimal("500000.00"));
        agency.setCashBalance(BigDecimal.ZERO);
        agency.setStatus(AgencyStatus.ACTIVE);

        return agencyDAO.save(agency);
    }

    /**
     * Met à jour une agence existante.
     */
    public Agency updateAgency(Long id, AgencyRequestDTO dto) {
        Agency agency = findById(id);

        if (dto.getName() != null && !dto.getName().isBlank()) {
            agency.setName(dto.getName().trim());
        }
        if (dto.getAddress() != null && !dto.getAddress().isBlank()) {
            agency.setAddress(dto.getAddress().trim());
        }
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            agency.setCity(dto.getCity().trim());
        }
        if (dto.getPhone() != null) {
            agency.setPhone(dto.getPhone().trim());
        }
        if (dto.getEmail() != null) {
            agency.setEmail(dto.getEmail().trim().toLowerCase());
        }
        if (dto.getDailyLimit() != null) {
            if (dto.getDailyLimit().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("La limite journalière ne peut pas être négative");
            }
            agency.setDailyLimit(dto.getDailyLimit());
        }
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            String newCode = dto.getCode().trim().toUpperCase();
            if (!agency.getCode().equals(newCode)) {
                if (agencyDAO.findByCode(newCode) != null) {
                    throw new BusinessException("Ce code d'agence est déjà utilisé");
                }
                agency.setCode(newCode);
            }
        }

        return agencyDAO.update(agency);
    }

    /**
     * Change le statut de l'agence (ACTIVE, SUSPENDED, CLOSED).
     */
    public void changeStatus(Long id, AgencyStatus status) {
        if (status == null) {
            throw new BusinessException("Le statut est obligatoire");
        }
        Agency agency = findById(id);
        agency.setStatus(status);
        agencyDAO.update(agency);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTION DE LA CAISSE (CASH OPERATIONS)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Alimente la caisse de l'agence.
     */
    public void addCash(Long agencyId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant d'alimentation doit être strictement positif");
        }
        Agency agency = findById(agencyId);
        agency.setCashBalance(agency.getCashBalance().add(amount));
        agencyDAO.update(agency);
    }

    /**
     * Retire du cash de la caisse de l'agence.
     */
    public void removeCash(Long agencyId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de retrait doit être strictement positif");
        }
        Agency agency = findById(agencyId);
        if (agency.getCashBalance().compareTo(amount) < 0) {
            throw new BusinessException("Solde de caisse insuffisant (Solde actuel: " + agency.getCashBalance() + " MAD)");
        }
        agency.setCashBalance(agency.getCashBalance().subtract(amount));
        agencyDAO.update(agency);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFECTATION DES AGENTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Assigne un agent à une agence.
     */
    public void assignAgent(Long agencyId, Long agentId) {
        Agency agency = findById(agencyId);
        User agent = userDAO.findById(agentId);

        if (agent == null) {
            throw new BusinessException("Agent introuvable");
        }
        if (agent.getRole() != User.UserRole.AGENCY_AGENT) {
            throw new BusinessException("L'utilisateur n'a pas le rôle d'agent de guichet (AGENCY_AGENT)");
        }
        if (agent.getStatus() != User.AccountStatus.ACTIVE) {
            throw new BusinessException("Le compte de l'agent n'est pas actif (" + agent.getStatus() + ")");
        }

        agent.setAgency(agency);
        userDAO.update(agent);
    }

    /**
     * Retire un agent de son agence actuelle.
     */
    public void removeAgent(Long agentId) {
        User agent = userDAO.findById(agentId);
        if (agent == null) {
            throw new BusinessException("Agent introuvable");
        }
        if (agent.getRole() != User.UserRole.AGENCY_AGENT) {
            throw new BusinessException("L'utilisateur n'est pas un agent de guichet");
        }

        agent.setAgency(null);
        userDAO.update(agent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TABLEAU DE BORD PAR AGENCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construit le tableau de bord d'une agence avec ses KPIs.
     */
    public AgencyDashboardDTO getDashboard(Long agencyId) {
        Agency agency = findById(agencyId);

        AgencyDashboardDTO dashboard = new AgencyDashboardDTO();

        // Informations de l'agence
        dashboard.setAgencyId(agency.getId());
        dashboard.setAgencyCode(agency.getCode());
        dashboard.setAgencyName(agency.getName());
        dashboard.setStatus(agency.getStatus());
        dashboard.setCashBalance(agency.getCashBalance());
        dashboard.setDailyLimit(agency.getDailyLimit());

        // KPIs transferts
        dashboard.setTotalTransfers(transferDAO.countAllByAgency(agencyId));
        dashboard.setPendingTransfers(transferDAO.countByAgencyAndStatus(agencyId, TransferStatus.PENDING));
        dashboard.setConfirmedTransfers(transferDAO.countByAgencyAndStatus(agencyId, TransferStatus.CONFIRMED));
        dashboard.setPaidTransfers(transferDAO.countByAgencyAndStatus(agencyId, TransferStatus.PAID));
        dashboard.setCancelledTransfers(transferDAO.countByAgencyAndStatus(agencyId, TransferStatus.CANCELLED));

        // Volumes financiers
        dashboard.setTotalVolumeSent(transferDAO.getTotalVolumeSentByAgency(agencyId));
        dashboard.setTotalVolumeReceived(transferDAO.getTotalVolumeReceivedByAgency(agencyId));
        dashboard.setTotalFeesCollected(transferDAO.getTotalFeesByAgency(agencyId));

        // Nombre d'agents
        List<User> agents = userDAO.findByAgency(agencyId);
        dashboard.setAgentCount(agents != null ? agents.size() : 0);

        return dashboard;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTE DES AGENTS D'UNE AGENCE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne la liste des agents affectés à une agence.
     */
    public List<AgentDTO> getAgentsByAgency(Long agencyId) {
        // Valide que l'agence existe
        findById(agencyId);

        List<User> agents = userDAO.findByAgency(agencyId);
        List<AgentDTO> result = new ArrayList<>();
        for (User agent : agents) {
            result.add(agentToDTO(agent));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTATIONS
    // ─────────────────────────────────────────────────────────────────────────

    public Agency findById(Long id) {
        Agency agency = agencyDAO.findById(id);
        if (agency == null) {
            throw new BusinessException("Agence introuvable (id=" + id + ")");
        }
        return agency;
    }

    public List<Agency> findAll() {
        return agencyDAO.findAll();
    }

    public List<Agency> findAllActive() {
        return agencyDAO.findAllActive();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private void validateAgencyRequest(AgencyRequestDTO dto) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            throw new BusinessException("Le code de l'agence est obligatoire");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("Le nom de l'agence est obligatoire");
        }
        if (dto.getAddress() == null || dto.getAddress().isBlank()) {
            throw new BusinessException("L'adresse de l'agence est obligatoire");
        }
        if (dto.getCity() == null || dto.getCity().isBlank()) {
            throw new BusinessException("La ville de l'agence est obligatoire");
        }
        if (dto.getDailyLimit() != null && dto.getDailyLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("La limite journalière ne peut pas être négative");
        }
    }

    private AgentDTO agentToDTO(User agent) {
        AgentDTO dto = new AgentDTO();
        dto.setId(agent.getId());
        dto.setFirstName(agent.getFirstName());
        dto.setLastName(agent.getLastName());
        dto.setEmail(agent.getEmail());
        dto.setPhone(agent.getPhone());
        dto.setStatus(agent.getStatus());
        dto.setCreatedAt(agent.getCreatedAt());
        dto.setLastLoginAt(agent.getLastLoginAt());
        return dto;
    }
}
