package ma.transfert.service;

import ma.transfert.dao.AgencyDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.dto.AgencyRequestDTO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.Agency;
import ma.transfert.model.Agency.AgencyStatus;
import ma.transfert.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DEV-4 — AgencyService")
class AgencyServiceTest {

    @Mock
    private AgencyDAO agencyDAO;

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AgencyService agencyService;

    // Helper
    private Agency buildAgency(Long id, String code, String name) {
        Agency agency = new Agency();
        agency.setId(id);
        agency.setCode(code);
        agency.setName(name);
        agency.setAddress("123, Rue des Fleurs");
        agency.setCity("Casablanca");
        agency.setCashBalance(BigDecimal.ZERO);
        agency.setDailyLimit(new BigDecimal("500000.00"));
        agency.setStatus(AgencyStatus.ACTIVE);
        return agency;
    }

    private User buildAgent(Long id, String email, User.UserRole role, User.AccountStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        user.setFirstName("Hassan");
        user.setLastName("Alami");
        return user;
    }

    @Test
    @DisplayName("createAgency() crée l'agence avec des données valides")
    void createAgency_validData_returnsAgency() {
        AgencyRequestDTO dto = new AgencyRequestDTO();
        dto.setCode("AGC-CAS-01");
        dto.setName("Agence Casa Oasis");
        dto.setAddress("Boulevard Oasis");
        dto.setCity("Casablanca");
        dto.setDailyLimit(new BigDecimal("100000.00"));

        when(agencyDAO.findByCode("AGC-CAS-01")).thenReturn(null);
        Agency saved = buildAgency(1L, "AGC-CAS-01", "Agence Casa Oasis");
        when(agencyDAO.save(any(Agency.class))).thenReturn(saved);

        Agency result = agencyService.createAgency(dto);

        assertNotNull(result);
        assertEquals("AGC-CAS-01", result.getCode());
        verify(agencyDAO).save(any(Agency.class));
    }

    @Test
    @DisplayName("createAgency() lève BusinessException si le code est déjà pris")
    void createAgency_duplicateCode_throwsBusinessException() {
        AgencyRequestDTO dto = new AgencyRequestDTO();
        dto.setCode("AGC-CAS-01");
        dto.setName("Agence Casa Oasis");
        dto.setAddress("Boulevard Oasis");
        dto.setCity("Casablanca");

        when(agencyDAO.findByCode("AGC-CAS-01")).thenReturn(buildAgency(1L, "AGC-CAS-01", "Existante"));

        assertThrows(BusinessException.class, () -> agencyService.createAgency(dto));
        verify(agencyDAO, never()).save(any(Agency.class));
    }

    @Test
    @DisplayName("createAgency() lève BusinessException si des champs obligatoires sont manquants")
    void createAgency_missingFields_throwsBusinessException() {
        AgencyRequestDTO dto = new AgencyRequestDTO();
        dto.setCode(""); 
        dto.setName("Agence Casa Oasis");
        dto.setAddress("Boulevard Oasis");
        dto.setCity("Casablanca");

        assertThrows(BusinessException.class, () -> agencyService.createAgency(dto));
    }

    @Test
    @DisplayName("updateAgency() modifie les champs autorisés")
    void updateAgency_validData_updatesFields() {
        Agency agency = buildAgency(1L, "AGC-CAS-01", "Agence Oasis");
        when(agencyDAO.findById(1L)).thenReturn(agency);
        when(agencyDAO.update(any(Agency.class))).thenReturn(agency);

        AgencyRequestDTO dto = new AgencyRequestDTO();
        dto.setName("Nouveau Nom Agence");
        dto.setCity("Rabat");

        Agency result = agencyService.updateAgency(1L, dto);

        assertNotNull(result);
        assertEquals("Nouveau Nom Agence", result.getName());
        assertEquals("Rabat", result.getCity());
        verify(agencyDAO).update(agency);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GESTION DE LA CAISSE (addCash/removeCash)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addCash() augmente le solde de la caisse")
    void addCash_validAmount_increasesBalance() {
        Agency agency = buildAgency(1L, "AGC-CAS-01", "Agence Oasis");
        when(agencyDAO.findById(1L)).thenReturn(agency);
        when(agencyDAO.update(any(Agency.class))).thenReturn(agency);

        agencyService.addCash(1L, new BigDecimal("15000.00"));

        assertEquals(0, agency.getCashBalance().compareTo(new BigDecimal("15000.00")));
        verify(agencyDAO).update(agency);
    }

    @Test
    @DisplayName("removeCash() diminue le solde si fonds suffisants")
    void removeCash_sufficientFunds_decreasesBalance() {
        Agency agency = buildAgency(1L, "AGC-CAS-01", "Agence Oasis");
        agency.setCashBalance(new BigDecimal("20000.00"));
        when(agencyDAO.findById(1L)).thenReturn(agency);
        when(agencyDAO.update(any(Agency.class))).thenReturn(agency);

        agencyService.removeCash(1L, new BigDecimal("5000.00"));

        assertEquals(0, agency.getCashBalance().compareTo(new BigDecimal("15000.00")));
        verify(agencyDAO).update(agency);
    }

    @Test
    @DisplayName("removeCash() lève BusinessException si solde de caisse insuffisant")
    void removeCash_insufficientFunds_throwsBusinessException() {
        Agency agency = buildAgency(1L, "AGC-CAS-01", "Agence Oasis");
        agency.setCashBalance(new BigDecimal("1000.00"));
        when(agencyDAO.findById(1L)).thenReturn(agency);

        assertThrows(BusinessException.class, () -> agencyService.removeCash(1L, new BigDecimal("5000.00")));
        verify(agencyDAO, never()).update(any(Agency.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFECTATION DES AGENTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignAgent() associe un agent valide à une agence")
    void assignAgent_validAgent_succeeds() {
        Agency agency = buildAgency(1L, "AGC-CAS-01", "Agence Oasis");
        User agent = buildAgent(2L, "agent@oasis.ma", User.UserRole.AGENCY_AGENT, User.AccountStatus.ACTIVE);

        when(agencyDAO.findById(1L)).thenReturn(agency);
        when(userDAO.findById(2L)).thenReturn(agent);
        when(userDAO.update(any(User.class))).thenReturn(agent);

        agencyService.assignAgent(1L, 2L);

        assertEquals(agency, agent.getAgency());
        verify(userDAO).update(agent);
    }

    @Test
    @DisplayName("assignAgent() lève BusinessException si l'utilisateur n'est pas agent")
    void assignAgent_invalidRole_throwsBusinessException() {
        Agency agency = buildAgency(1L, "AGC-CAS-01", "Agence Oasis");
        User client = buildAgent(2L, "client@test.ma", User.UserRole.INDIVIDUAL, User.AccountStatus.ACTIVE);

        when(agencyDAO.findById(1L)).thenReturn(agency);
        when(userDAO.findById(2L)).thenReturn(client);

        assertThrows(BusinessException.class, () -> agencyService.assignAgent(1L, 2L));
        verify(userDAO, never()).update(any(User.class));
    }
}
