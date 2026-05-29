package ma.transfert.service;

import jakarta.ejb.Stateless;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Stateless
public class FeeCalculatorService {
    
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("50000");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("50");
    
    public BigDecimal calculateFees(BigDecimal amount) {
        validateAmount(amount);
        
        if (amount.compareTo(new BigDecimal("1000")) <= 0) {
            return new BigDecimal("25.00");
        } else if (amount.compareTo(new BigDecimal("5000")) <= 0) {
            return new BigDecimal("35.00");
        } else if (amount.compareTo(new BigDecimal("10000")) <= 0) {
            return new BigDecimal("50.00");
        } else if (amount.compareTo(new BigDecimal("20000")) <= 0) {
            return amount.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP);
        } else {
            return amount.multiply(new BigDecimal("0.005")).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
    public void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Le montant est requis");
        }
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Le montant minimum est de " + MIN_AMOUNT + " MAD");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Le montant maximum est de " + MAX_AMOUNT + " MAD");
        }
    }
    
    public BigDecimal getMaxAmount() { return MAX_AMOUNT; }
    public BigDecimal getMinAmount() { return MIN_AMOUNT; }
}