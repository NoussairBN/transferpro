package ma.transfert.dto;

import java.math.BigDecimal;

public class CashOperationRequestDTO {
    private BigDecimal amount;

    public CashOperationRequestDTO() {}

    public CashOperationRequestDTO(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
