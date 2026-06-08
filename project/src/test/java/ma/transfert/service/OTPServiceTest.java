package ma.transfert.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OTPServiceTest {

    private OTPService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OTPService();
    }

    @Test
    @DisplayName("OTP a exactement 8 caractères")
    void otp_hasLength8() {
        assertEquals(8, otpService.generateOTP().length());
    }

    @Test
    @DisplayName("OTP contient uniquement des chiffres")
    void otp_onlyDigits() {
        assertTrue(otpService.generateOTP().matches("\\d{8}"));
    }

    @Test
    @DisplayName("OTP n'est pas null")
    void otp_notNull() {
        assertNotNull(otpService.generateOTP());
    }

    @RepeatedTest(50)
    @DisplayName("OTP toujours valide sur 50 générations")
    void otp_alwaysValid() {
        String otp = otpService.generateOTP();
        assertEquals(8, otp.length());
        assertTrue(otp.matches("\\d+"));
    }

    @Test
    @DisplayName("OTPs uniques sur 500 générations")
    void otp_unique() {
        Set<String> otps = new HashSet<>();
        for (int i = 0; i < 500; i++) otps.add(otpService.generateOTP());
        assertTrue(otps.size() > 490);
    }
}