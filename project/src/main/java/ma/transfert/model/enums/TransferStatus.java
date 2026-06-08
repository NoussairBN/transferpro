package ma.transfert.model.enums;

public enum TransferStatus {
    PENDING("En attente de confirmation"),
    CONFIRMED("Confirmé, fonds réservés"),
    AVAILABLE("Disponible pour retrait"),
    PAID("Payé - Terminé"),
    EXPIRED("Expiré - Non retiré"),
    CANCELLED("Annulé par l'expéditeur");
    
    private final String description;
    
    TransferStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == PAID || this == EXPIRED || this == CANCELLED;
    }
    
    public boolean canBeCancelled() {
        return this == PENDING || this == CONFIRMED;
    }
}