package ma.transfert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PaymentRequestDTO{
    
    @NotBlank(message = "Le code de suivi est requis")
    @Pattern(regexp = "^TRF-\\d{8}-[A-Z0-9]{8}$", message = "Format de code de suivi invalide")
    private String trackingCode;
    
    @NotBlank(message = "Le code OTP est requis")
    @Pattern(regexp = "^[0-9]{8}$", message = "Le code OTP doit contenir 8 chiffres")
    private String otp;
    
    // Getters and Setters
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}