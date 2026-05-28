package ma.transfert.service;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Stateless
public class NotificationService {
    
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());
    
    @Asynchronous
    public void sendOTPNotification(String phone, String email, String otp, String trackingCode) {
        String message = String.format("Votre code OTP pour le transfert %s est: %s. Valable 24h.", trackingCode, otp);
        
        if (phone != null && !phone.isEmpty()) {
            logger.info(String.format("📱 SMS envoyé à %s: %s", phone, message));
            // In production: call SMS API (Twilio, etc.)
        }
        
        if (email != null && !email.isEmpty()) {
            logger.info(String.format("📧 Email envoyé à %s: %s", email, message));
            // In production: send email via JavaMail
        }
        
        // Log to database would go here
        logNotification(phone, email, "OTP_SENT", trackingCode);
    }
    
    @Asynchronous
    public void sendPaymentConfirmation(String phone, String email, String trackingCode, BigDecimal amount) {
        String message = String.format("Votre transfert %s de %.2f MAD a été payé avec succès.", trackingCode, amount);
        
        if (phone != null && !phone.isEmpty()) {
            logger.info(String.format("📱 SMS à %s: %s", phone, message));
        }
        
        if (email != null && !email.isEmpty()) {
            logger.info(String.format("📧 Email à %s: %s", email, message));
        }
        
        logNotification(phone, email, "PAYMENT_CONFIRMED", trackingCode);
    }
    
    @Asynchronous
    public void sendTransferCreatedNotification(String phone, String email, String trackingCode, BigDecimal amount) {
        String message = String.format("Transfert %s créé pour %.2f MAD. Suivez-le sur notre application.", trackingCode, amount);
        
        if (phone != null && !phone.isEmpty()) {
            logger.info(String.format("📱 SMS à %s: %s", phone, message));
        }
        
        if (email != null && !email.isEmpty()) {
            logger.info(String.format("📧 Email à %s: %s", email, message));
        }
        
        logNotification(phone, email, "TRANSFER_CREATED", trackingCode);
    }
    
    @Asynchronous
    public void sendTransferExpiredNotification(String phone, String email, String trackingCode) {
        String message = String.format("Votre transfert %s a expiré car non retiré dans les 30 jours.", trackingCode);
        
        if (phone != null && !phone.isEmpty()) {
            logger.info(String.format("📱 SMS à %s: %s", phone, message));
        }
        
        if (email != null && !email.isEmpty()) {
            logger.info(String.format("📧 Email à %s: %s", email, message));
        }
        
        logNotification(phone, email, "TRANSFER_EXPIRED", trackingCode);
    }
    
    private void logNotification(String phone, String email, String type, String trackingCode) {
        logger.info(String.format("[NOTIFICATION] Type: %s, Phone: %s, Email: %s, Transfer: %s, Time: %s",
            type, phone, email, trackingCode, LocalDateTime.now()));
    }
}