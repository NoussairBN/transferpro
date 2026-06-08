package ma.transfert.service;

import ma.transfert.dao.TransferDAO;
import ma.transfert.dto.TransferCreateDTO;
import ma.transfert.dto.TransferDTO;
import ma.transfert.model.Agency;
import ma.transfert.model.Transfer;
import ma.transfert.model.User;
import ma.transfert.model.enums.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private TransferDAO transferDAO;
    @Mock private OTPService otpService;
    @Mock private FeeCalculatorService feeCalculator;
    @InjectMocks private TransferService transferService;

    private Agency agency;
    private User user;

    @BeforeEach
    void setUp() {
        agency = new Agency();
        agency.setId(1L);
        agency.setName("Agence Casa");
        agency.setCode("AGC-01");

        user = new User();
        user.setId(1L);
        user.setEmail("agent@test.ma");
    }

    private Transfer makeTransfer(String code, TransferStatus status, BigDecimal amount) {
        Transfer t = new Transfer();
        t.setId(1L);
        t.setTrackingCode(code);
        t.setStatus(status);
        t.setAmount(amount);
        t.setFees(new BigDecimal("25.00"));
        t.setOtpCode("12345678");
        t.setSenderName("Ali");
        t.setReceiverName("Sara");
        t.setSenderPhone("0612345678");
        t.setReceiverPhone("0698765432");
        t.setSendingAgency(agency);
        t.setUser(user);
        return t;
    }

    @Test
    @DisplayName("createTransfer() retourne un DTO avec statut PENDING")
    void createTransfer_returnsDTO() {
        TransferCreateDTO dto = new TransferCreateDTO();
        dto.setAmount(new BigDecimal("500"));
        dto.setSenderName("Ali"); dto.setSenderPhone("06");
        dto.setReceiverName("Sara"); dto.setReceiverPhone("07");

        when(otpService.generateOTP()).thenReturn("12345678");
        when(feeCalculator.calculateFees(any())).thenReturn(new BigDecimal("25.00"));
        when(transferDAO.save(any())).thenReturn(
                makeTransfer("TRF-001", TransferStatus.PENDING, new BigDecimal("500")));

        TransferDTO result = transferService.createTransfer(dto, agency, user);

        assertNotNull(result);
        assertEquals(TransferStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("25.00"), result.getFees());
        verify(transferDAO).save(any());
    }

    @Test
    @DisplayName("confirmTransfer() PENDING → CONFIRMED")
    void confirmTransfer_success() {
        Transfer t = makeTransfer("TRF-002", TransferStatus.PENDING, new BigDecimal("500"));
        when(transferDAO.findByTrackingCode("TRF-002")).thenReturn(t);
        when(transferDAO.update(any())).thenAnswer(i -> i.getArgument(0));

        TransferDTO result = transferService.confirmTransfer("TRF-002");
        assertEquals(TransferStatus.CONFIRMED, result.getStatus());
    }

    @Test
    @DisplayName("confirmTransfer() code inconnu → exception")
    void confirmTransfer_notFound_throws() {
        when(transferDAO.findByTrackingCode("INCONNU")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> transferService.confirmTransfer("INCONNU"));
    }

    @Test
    @DisplayName("payTransfer() bon OTP → PAID")
    void payTransfer_validOTP_paid() {
        Transfer t = makeTransfer("TRF-003", TransferStatus.AVAILABLE, new BigDecimal("500"));
        when(transferDAO.findByTrackingCode("TRF-003")).thenReturn(t);
        when(transferDAO.update(any())).thenAnswer(i -> i.getArgument(0));

        TransferDTO result = transferService.payTransfer("TRF-003", "12345678", agency);
        assertEquals(TransferStatus.PAID, result.getStatus());
    }

    @Test
    @DisplayName("payTransfer() mauvais OTP → exception")
    void payTransfer_wrongOTP_throws() {
        Transfer t = makeTransfer("TRF-004", TransferStatus.AVAILABLE, new BigDecimal("500"));
        when(transferDAO.findByTrackingCode("TRF-004")).thenReturn(t);
        assertThrows(RuntimeException.class,
                () -> transferService.payTransfer("TRF-004", "00000000", agency));
    }

    @Test
    @DisplayName("cancelTransfer() PENDING → CANCELLED")
    void cancelTransfer_success() {
        Transfer t = makeTransfer("TRF-005", TransferStatus.PENDING, new BigDecimal("500"));
        when(transferDAO.findByTrackingCode("TRF-005")).thenReturn(t);
        when(transferDAO.update(any())).thenAnswer(i -> i.getArgument(0));

        TransferDTO result = transferService.cancelTransfer("TRF-005");
        assertEquals(TransferStatus.CANCELLED, result.getStatus());
    }

    @Test
    @DisplayName("cancelTransfer() code inconnu → exception")
    void cancelTransfer_notFound_throws() {
        when(transferDAO.findByTrackingCode("XXX")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> transferService.cancelTransfer("XXX"));
    }
}