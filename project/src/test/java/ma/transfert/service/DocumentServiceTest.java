package ma.transfert.service;

import ma.transfert.dao.DocumentDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.model.*;
import ma.transfert.service.doc.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentDAO documentDAO;
    @Mock private UserDAO userDAO;
    @InjectMocks private DocumentService documentService;

    private User makeUser(Long id) {
        User u = new User();
        u.setEmail("user@test.ma");
        u.setKycStatus(User.KycStatus.PENDING);
        return u;
    }

    @Test
    @DisplayName("uploadDocument() accepte application/pdf")
    void upload_pdf_ok() throws Exception {
        when(userDAO.findById(1L)).thenReturn(makeUser(1L));
        Document saved = new Document();
        saved.setMimeType("application/pdf");
        when(documentDAO.save(any())).thenReturn(saved);

        Document result = documentService.uploadDocument(
                new ByteArrayInputStream("data".getBytes()),
                "cin.pdf", "application/pdf", DocumentType.CNI_RECTO, 1L);

        assertNotNull(result);
        assertEquals("application/pdf", result.getMimeType());
    }

    @Test
    @DisplayName("uploadDocument() refuse image/gif → exception")
    void upload_gif_rejected() {
        assertThrows(IllegalArgumentException.class, () ->
                documentService.uploadDocument(
                        new ByteArrayInputStream("gif".getBytes()),
                        "anim.gif", "image/gif", DocumentType.CNI_RECTO, 1L));
    }

    @Test
    @DisplayName("uploadDocument() refuse application/exe → exception")
    void upload_exe_rejected() {
        assertThrows(IllegalArgumentException.class, () ->
                documentService.uploadDocument(
                        new ByteArrayInputStream("exe".getBytes()),
                        "virus.exe", "application/exe", DocumentType.CNI_RECTO, 1L));
    }

    @Test
    @DisplayName("uploadDocument() utilisateur inexistant → exception")
    void upload_unknownUser_throws() {
        when(userDAO.findById(99L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () ->
                documentService.uploadDocument(
                        new ByteArrayInputStream("data".getBytes()),
                        "doc.pdf", "application/pdf", DocumentType.CNI_RECTO, 99L));
    }

    @Test
    @DisplayName("validateDocument() → statut VALIDATED")
    void validate_changesStatus() {
        User user = makeUser(1L);
        Document doc = new Document();
        doc.setStatus(DocumentStatus.PENDING);
        doc.setType(DocumentType.CNI_RECTO);
        doc.setOwner(user);

        when(documentDAO.findById(1L)).thenReturn(doc);
        when(documentDAO.update(any())).thenAnswer(i -> i.getArgument(0));
        when(documentDAO.findByOwner(any())).thenReturn(List.of(doc));

        Document result = documentService.validateDocument(1L, "OK");
        assertEquals(DocumentStatus.VALIDATED, result.getStatus());
    }

    @Test
    @DisplayName("rejectDocument() → statut REJECTED")
    void reject_changesStatus() {
        User user = makeUser(1L);
        Document doc = new Document();
        doc.setStatus(DocumentStatus.PENDING);
        doc.setOwner(user);

        when(documentDAO.findById(1L)).thenReturn(doc);
        when(documentDAO.update(any())).thenAnswer(i -> i.getArgument(0));

        Document result = documentService.rejectDocument(1L, "Flou");
        assertEquals(DocumentStatus.REJECTED, result.getStatus());
        verify(userDAO).update(user);
    }
}