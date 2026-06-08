package ma.transfert.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotificationServiceTest {

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
    }

    @Test
    @DisplayName("sendOTPNotification() ne lève pas d'exception")
    void sendOTP_noException() {
        assertDoesNotThrow(() ->
                service.sendOTPNotification("0612345678", "test@test.ma", "12345678", "TRF-001"));
    }

    @Test
    @DisplayName("sendOTPNotification() sans phone ni email — pas de crash")
    void sendOTP_nullParams_noException() {
        assertDoesNotThrow(() ->
                service.sendOTPNotification(null, null, "12345678", "TRF-001"));
    }

    @Test
    @DisplayName("sendPaymentConfirmation() ne lève pas d'exception")
    void sendPayment_noException() {
        assertDoesNotThrow(() ->
                service.sendPaymentConfirmation("06", "a@b.ma", "TRF-001", new BigDecimal("500")));
    }

    @Test
    @DisplayName("sendTransferCreatedNotification() ne lève pas d'exception")
    void sendCreated_noException() {
        assertDoesNotThrow(() ->
                service.sendTransferCreatedNotification("06", "a@b.ma", "TRF-001", new BigDecimal("200")));
    }

    @Test
    @DisplayName("sendTransferExpiredNotification() ne lève pas d'exception")
    void sendExpired_noException() {
        assertDoesNotThrow(() ->
                service.sendTransferExpiredNotification("06", "a@b.ma", "TRF-001"));
    }

    @Test
    @DisplayName("sendCancellationNotification() ne lève pas d'exception")
    void sendCancellation_noException() {
        assertDoesNotThrow(() ->
                service.sendCancellationNotification("06", "a@b.ma", "TRF-001"));
    }
}