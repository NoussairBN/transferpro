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

    private String email;
    private String password;

    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            AuthService.LoginResult result = authService.login(email, password);

            // Stocker userId et token dans la session HTTP
            ctx.getExternalContext().getSessionMap().put("userId", result.getUserId());
            ctx.getExternalContext().getSessionMap().put("userEmail", result.getEmail());
            ctx.getExternalContext().getSessionMap().put("userFullName", result.getFullName());
            ctx.getExternalContext().getSessionMap().put("userRole", result.getRole());
            ctx.getExternalContext().getSessionMap().put("jwtToken", result.getToken());

            return "dashboard?faces-redirect=true";

        } catch (Exception e) {
            ctx.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Email ou mot de passe incorrect", null));
            return null;
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String logout() {
    FacesContext.getCurrentInstance()
            .getExternalContext()
            .invalidateSession();
    return "login?faces-redirect=true"; }
}