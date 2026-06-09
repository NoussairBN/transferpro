package ma.transfert.jsf;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import ma.transfert.service.AuthService;

@Named("loginBean")
@RequestScoped
public class LoginBean {

    @EJB
    private AuthService authService;

    // Login fields
    private String email;
    private String password;

    // Register fields
    private String regFirstName;
    private String regLastName;
    private String regEmail;
    private String regPhone;
    private String regPassword;
    private String regPasswordConfirm;

    private String extractMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        String msg = cause.getMessage();
        if (msg == null) return "Erreur inattendue";
        int colon = msg.lastIndexOf(':');
        return colon >= 0 ? msg.substring(colon + 1).trim() : msg;
    }

    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            AuthService.LoginResult result = authService.login(email, password);
            ctx.getExternalContext().getSessionMap().put("userId",       result.getUserId());
            ctx.getExternalContext().getSessionMap().put("userEmail",    result.getEmail());
            ctx.getExternalContext().getSessionMap().put("userFullName", result.getFullName());
            ctx.getExternalContext().getSessionMap().put("userRole",     result.getRole());
            ctx.getExternalContext().getSessionMap().put("jwtToken",     result.getToken());
            return "ADMIN".equals(result.getRole())
                ? "admin-dashboard?faces-redirect=true"
                : "user-dashboard?faces-redirect=true";
        } catch (Exception e) {
            ctx.addMessage("loginForm",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, extractMessage(e), null));
            return null;
        }
    }

    public String register() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (regFirstName == null || regFirstName.isBlank() ||
            regLastName  == null || regLastName.isBlank()  ||
            regEmail     == null || regEmail.isBlank()     ||
            regPhone     == null || regPhone.isBlank()     ||
            regPassword  == null || regPassword.isBlank()) {
            ctx.addMessage("registerForm",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Tous les champs sont obligatoires", null));
            return null;
        }
        if (!regPassword.equals(regPasswordConfirm)) {
            ctx.addMessage("registerForm",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Les mots de passe ne correspondent pas", null));
            return null;
        }
        try {
            AuthService.LoginResult result = authService.register(
                regFirstName.trim(), regLastName.trim(),
                regEmail.trim().toLowerCase(), regPhone.trim(), regPassword);
            ctx.getExternalContext().getSessionMap().put("userId",       result.getUserId());
            ctx.getExternalContext().getSessionMap().put("userEmail",    result.getEmail());
            ctx.getExternalContext().getSessionMap().put("userFullName", result.getFullName());
            ctx.getExternalContext().getSessionMap().put("userRole",     result.getRole());
            ctx.getExternalContext().getSessionMap().put("jwtToken",     result.getToken());
            return "user-dashboard?faces-redirect=true";
        } catch (Exception e) {
            ctx.addMessage("registerForm",
                new FacesMessage(FacesMessage.SEVERITY_ERROR, extractMessage(e), null));
            return null;
        }
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "login?faces-redirect=true";
    }

    // Login getters/setters
    public String getEmail()    { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }

    // Register getters/setters
    public String getRegFirstName()   { return regFirstName; }
    public void setRegFirstName(String v) { this.regFirstName = v; }
    public String getRegLastName()    { return regLastName; }
    public void setRegLastName(String v)  { this.regLastName = v; }
    public String getRegEmail()       { return regEmail; }
    public void setRegEmail(String v) { this.regEmail = v; }
    public String getRegPhone()       { return regPhone; }
    public void setRegPhone(String v) { this.regPhone = v; }
    public String getRegPassword()    { return regPassword; }
    public void setRegPassword(String v) { this.regPassword = v; }
    public String getRegPasswordConfirm() { return regPasswordConfirm; }
    public void setRegPasswordConfirm(String v) { this.regPasswordConfirm = v; }
}
