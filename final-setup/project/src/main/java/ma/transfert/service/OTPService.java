package ma.transfert.service;

import jakarta.ejb.Stateless;
import java.security.SecureRandom;

@Stateless
public class OTPService {
    
    private static final int OTP_LENGTH = 8;
    private static final SecureRandom secureRandom = new SecureRandom();
    
    public String generateOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}