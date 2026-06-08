package ma.transfert.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FeeCalculatorServiceTest {

    private FeeCalculatorService service;

    @BeforeEach
    void setUp() {
        service = new FeeCalculatorService();
    }

    @Test
    @DisplayName("500 MAD → frais 25 MAD")
    void fees_500_returns25() {
        assertEquals(new BigDecimal("25.00"), service.calculateFees(new BigDecimal("500")));
    }

    @Test
    @DisplayName("3000 MAD → frais 35 MAD")
    void fees_3000_returns35() {
        assertEquals(new BigDecimal("35.00"), service.calculateFees(new BigDecimal("3000")));
    }

    @Test
    @DisplayName("7000 MAD → frais 50 MAD")
    void fees_7000_returns50() {
        assertEquals(new BigDecimal("50.00"), service.calculateFees(new BigDecimal("7000")));
    }

    @Test
    @DisplayName("Montant null → exception")
    void fees_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.calculateFees(null));
    }

    @Test
    @DisplayName("49 MAD → exception (sous le minimum)")
    void fees_belowMin_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateFees(new BigDecimal("49")));
    }
}