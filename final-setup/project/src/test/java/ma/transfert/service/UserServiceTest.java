package ma.transfert.service;

import ma.transfert.dao.UserDAO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.User;
import ma.transfert.security.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UserService.
 * Utilise Mockito pour isoler UserDAO et PasswordUtil — pas de DB.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T3 — UserService")
class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private UserService userService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, String phone) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Hassan");
        u.setLastName("Alami");
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash("$2a$12$hashfake");
        u.setRole(User.UserRole.INDIVIDUAL);
        u.setStatus(User.AccountStatus.ACTIVE);
        return u;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() crée l'utilisateur quand toutes les données sont valides")
    void register_validData_returnsUser() {
        when(userDAO.findByEmail(anyString())).thenReturn(null);
        when(userDAO.findByPhone(anyString())).thenReturn(null);
        when(passwordUtil.hash(anyString())).thenReturn("$2a$12$hashedpw");

        User saved = buildUser(1L, "hassan@test.ma", "0612345678");
        when(userDAO.save(any(User.class))).thenReturn(saved);

        User result = userService.register("Hassan", "Alami",
                "hassan@test.ma", "0612345678",
                "motdepasse123", User.UserRole.INDIVIDUAL);

        assertNotNull(result);
        assertEquals("hassan@test.ma", result.getEmail());
        verify(userDAO).save(any(User.class));
    }

    @Test
    @DisplayName("register() lève BusinessException si email déjà utilisé")
    void register_duplicateEmail_throwsBusinessException() {
        when(userDAO.findByEmail("hassan@test.ma")).thenReturn(buildUser(1L, "hassan@test.ma", "0612345678"));

        assertThrows(BusinessException.class, () ->
                userService.register("Hassan", "Alami", "hassan@test.ma",
                        "0611111111", "motdepasse123", User.UserRole.INDIVIDUAL));

        verify(userDAO, never()).save(any());
    }

    @Test
    @DisplayName("register() lève BusinessException si téléphone déjà utilisé")
    void register_duplicatePhone_throwsBusinessException() {
        when(userDAO.findByEmail(anyString())).thenReturn(null);
        when(userDAO.findByPhone("0612345678")).thenReturn(buildUser(2L, "autre@test.ma", "0612345678"));

        assertThrows(BusinessException.class, () ->
                userService.register("Hassan", "Alami", "nouveau@test.ma",
                        "0612345678", "motdepasse123", User.UserRole.INDIVIDUAL));

        verify(userDAO, never()).save(any());
    }

    @Test
    @DisplayName("register() lève BusinessException si email invalide")
    void register_invalidEmail_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                userService.register("Hassan", "Alami", "pasunemail",
                        "0612345678", "motdepasse123", User.UserRole.INDIVIDUAL));
    }

    @Test
    @DisplayName("register() lève BusinessException si mot de passe trop court")
    void register_shortPassword_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                userService.register("Hassan", "Alami", "hassan@test.ma",
                        "0612345678", "court", User.UserRole.INDIVIDUAL));
    }

    @Test
    @DisplayName("register() lève BusinessException si prénom vide")
    void register_blankFirstName_throwsBusinessException() {
        assertThrows(BusinessException.class, () ->
                userService.register("", "Alami", "hassan@test.ma",
                        "0612345678", "motdepasse123", User.UserRole.INDIVIDUAL));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findById()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() retourne l'utilisateur si trouvé")
    void findById_existingUser_returnsUser() {
        User user = buildUser(1L, "hassan@test.ma", "0612345678");
        when(userDAO.findById(1L)).thenReturn(user);

        User result = userService.findById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("findById() lève BusinessException si utilisateur non trouvé")
    void findById_notFound_throwsBusinessException() {
        when(userDAO.findById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> userService.findById(99L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // changePassword()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword() réussit si l'ancien mot de passe est correct")
    void changePassword_correctOldPassword_succeeds() {
        User user = buildUser(1L, "hassan@test.ma", "0612345678");
        when(userDAO.findById(1L)).thenReturn(user);
        when(passwordUtil.verify("ancienmdp", "$2a$12$hashfake")).thenReturn(true);
        when(passwordUtil.hash("nouveaumdp123")).thenReturn("$2a$12$newhash");
        when(userDAO.update(any())).thenReturn(user);

        assertDoesNotThrow(() -> userService.changePassword(1L, "ancienmdp", "nouveaumdp123"));
        verify(userDAO).update(any());
    }

    @Test
    @DisplayName("changePassword() lève BusinessException si l'ancien mot de passe est incorrect")
    void changePassword_wrongOldPassword_throwsBusinessException() {
        User user = buildUser(1L, "hassan@test.ma", "0612345678");
        when(userDAO.findById(1L)).thenReturn(user);
        when(passwordUtil.verify("mauvaismdp", "$2a$12$hashfake")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, "mauvaismdp", "nouveaumdp123"));
        verify(userDAO, never()).update(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // suspendUser() / activateUser()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("suspendUser() change le statut à SUSPENDED")
    void suspendUser_changesStatusToSuspended() {
        User user = buildUser(1L, "hassan@test.ma", "0612345678");
        when(userDAO.findById(1L)).thenReturn(user);
        when(userDAO.update(any())).thenReturn(user);

        userService.suspendUser(1L);

        assertEquals(User.AccountStatus.SUSPENDED, user.getStatus());
        verify(userDAO).update(user);
    }

    @Test
    @DisplayName("activateUser() change le statut à ACTIVE")
    void activateUser_changesStatusToActive() {
        User user = buildUser(1L, "hassan@test.ma", "0612345678");
        user.setStatus(User.AccountStatus.SUSPENDED);
        when(userDAO.findById(1L)).thenReturn(user);
        when(userDAO.update(any())).thenReturn(user);

        userService.activateUser(1L);

        assertEquals(User.AccountStatus.ACTIVE, user.getStatus());
    }
}
