package ma.transfert.service;

import ma.transfert.dao.UserDAO;
import ma.transfert.exception.BusinessException;
import ma.transfert.model.User;
import ma.transfert.security.JWTUtil;
import ma.transfert.security.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthService.
 * Mockito isole UserDAO, PasswordUtil, JWTUtil et UserService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T4 — AuthService")
class AuthServiceTest {

    @Mock private UserDAO userDAO;
    @Mock private PasswordUtil passwordUtil;
    @Mock private JWTUtil jwtUtil;
    @Mock private UserService userService;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setFirstName("Hassan");
        activeUser.setLastName("Alami");
        activeUser.setEmail("hassan@test.ma");
        activeUser.setPhone("0612345678");
        activeUser.setPasswordHash("$2a$12$hashfake");
        activeUser.setRole(User.UserRole.INDIVIDUAL);
        activeUser.setStatus(User.AccountStatus.ACTIVE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // login()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() retourne un LoginResult avec token valide")
    void login_validCredentials_returnsLoginResult() {
        when(userDAO.findByEmail("hassan@test.ma")).thenReturn(activeUser);
        when(passwordUtil.verify("motdepasse123", "$2a$12$hashfake")).thenReturn(true);
        when(userDAO.update(any())).thenReturn(activeUser);
        when(jwtUtil.generateToken(1L, "hassan@test.ma", "INDIVIDUAL", "Hassan Alami"))
                .thenReturn("jwt.token.fake");

        AuthService.LoginResult result = authService.login("hassan@test.ma", "motdepasse123");

        assertNotNull(result);
        assertEquals("jwt.token.fake", result.getToken());
        assertEquals("hassan@test.ma", result.getEmail());
        assertEquals("INDIVIDUAL", result.getRole());
        verify(userDAO).update(any()); // lastLoginAt mis à jour
    }

    @Test
    @DisplayName("login() normalise l'email en minuscules")
    void login_uppercaseEmail_normalizes() {
        when(userDAO.findByEmail("hassan@test.ma")).thenReturn(activeUser);
        when(passwordUtil.verify(anyString(), anyString())).thenReturn(true);
        when(userDAO.update(any())).thenReturn(activeUser);
        when(jwtUtil.generateToken(any(), any(), any(), any())).thenReturn("tok");

        assertDoesNotThrow(() -> authService.login("HASSAN@TEST.MA", "motdepasse123"));
        verify(userDAO).findByEmail("hassan@test.ma"); // normalisé
    }

    @Test
    @DisplayName("login() lève BusinessException si email introuvable")
    void login_unknownEmail_throwsBusinessException() {
        when(userDAO.findByEmail(anyString())).thenReturn(null);
        assertThrows(BusinessException.class, () -> authService.login("inconnu@test.ma", "mdp"));
    }

    @Test
    @DisplayName("login() lève BusinessException si mot de passe incorrect")
    void login_wrongPassword_throwsBusinessException() {
        when(userDAO.findByEmail("hassan@test.ma")).thenReturn(activeUser);
        when(passwordUtil.verify("mauvais", "$2a$12$hashfake")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login("hassan@test.ma", "mauvais"));
        verify(jwtUtil, never()).generateToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("login() lève BusinessException si le compte est suspendu")
    void login_suspendedAccount_throwsBusinessException() {
        activeUser.setStatus(User.AccountStatus.SUSPENDED);
        when(userDAO.findByEmail("hassan@test.ma")).thenReturn(activeUser);
        when(passwordUtil.verify(anyString(), anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.login("hassan@test.ma", "motdepasse123"));
        verify(jwtUtil, never()).generateToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("login() lève BusinessException si email vide")
    void login_blankEmail_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> authService.login("", "mdp"));
    }

    @Test
    @DisplayName("login() lève BusinessException si mot de passe vide")
    void login_blankPassword_throwsBusinessException() {
        assertThrows(BusinessException.class, () -> authService.login("hassan@test.ma", ""));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() retourne un LoginResult avec token après inscription")
    void register_validData_returnsLoginResultWithToken() {
        when(userService.register(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(User.UserRole.class))).thenReturn(activeUser);
        when(jwtUtil.generateToken(1L, "hassan@test.ma", "INDIVIDUAL", "Hassan Alami"))
                .thenReturn("jwt.register.token");

        AuthService.LoginResult result = authService.register(
                "Hassan", "Alami", "hassan@test.ma", "0612345678", "motdepasse123"
        );

        assertNotNull(result);
        assertEquals("jwt.register.token", result.getToken());
    }

    @Test
    @DisplayName("register() propage la BusinessException de UserService si données invalides")
    void register_duplicateEmail_propagatesException() {
        when(userService.register(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(User.UserRole.class)))
                .thenThrow(new BusinessException("Cet email est déjà utilisé"));

        assertThrows(BusinessException.class, () ->
                authService.register("Hassan", "Alami", "hassan@test.ma", "0612345678", "mdp12345"));
    }
}
