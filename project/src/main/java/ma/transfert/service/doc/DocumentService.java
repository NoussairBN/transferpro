package ma.transfert.service.doc;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import ma.transfert.dao.DocumentDAO;
import ma.transfert.dao.TransferDAO;
import ma.transfert.dao.UserDAO;
import ma.transfert.model.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Stateless
public class DocumentService {

    private static final String STORAGE_DIR = "uploads/documents/";

    @EJB
    private DocumentDAO documentDAO;

    // ✅ NOUVEAU : injecte UserDAO de DEV-3
    @EJB
    private UserDAO userDAO;

    // ✅ NOUVEAU : injecte TransferDAO de DEV-2
    @EJB
    private TransferDAO transferDAO;

    /**
     * Upload d'un document KYC pour un utilisateur réel.
     * Remplace l'ancienne version qui ne faisait que stocker un Long ownerId.
     */
    @Transactional
    public Document uploadDocument(InputStream inputStream, String originalName,
                                   String mimeType, DocumentType type, Long userId) throws Exception {

        if (!isAllowedMimeType(mimeType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Formats acceptés : PDF, JPEG, PNG");
        }

        // ✅ Charger le vrai User depuis DEV-3
        User owner = userDAO.findById(userId);
        if (owner == null) {
            throw new IllegalArgumentException("Utilisateur introuvable : " + userId);
        }

        // Sauvegarde physique du fichier
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetPath = Paths.get(STORAGE_DIR, storedName);
        Files.createDirectories(targetPath.getParent());
        Files.copy(inputStream, targetPath);

        // Création de l'entité Document
        Document document = new Document();
        document.setOriginalName(originalName);
        document.setStoredName(storedName);
        document.setFilePath(targetPath.toString());
        document.setMimeType(mimeType);
        document.setType(type);
        document.setOwner(owner);          // ✅ relation réelle
        document.setStatus(DocumentStatus.PENDING);

        Document saved = documentDAO.save(document);

        // ✅ NOUVEAU : si c'est un doc KYC, repasser le statut user à PENDING
        // (en cas de re-upload après rejet)
        if (isKycDocument(type) && owner.getKycStatus() == User.KycStatus.REJECTED) {
            owner.setKycStatus(User.KycStatus.PENDING);
            userDAO.update(owner);
        }

        return saved;
    }

    /**
     * Upload d'un document lié à un transfert (ex: reçu signé).
     * ✅ NOUVEAU : utilise le vrai Transfer de DEV-2.
     */
    @Transactional
    public Document uploadTransferDocument(InputStream inputStream, String originalName,
                                           String mimeType, DocumentType type,
                                           Long userId, Long transferId) throws Exception {

        if (!isAllowedMimeType(mimeType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé");
        }

        User owner = userDAO.findById(userId);
        if (owner == null) throw new IllegalArgumentException("Utilisateur introuvable");

        // ✅ Charger le vrai Transfer depuis DEV-2
        Transfer transfer = transferDAO.findById(transferId);
        if (transfer == null) throw new IllegalArgumentException("Transfert introuvable : " + transferId);

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path targetPath = Paths.get(STORAGE_DIR, storedName);
        Files.createDirectories(targetPath.getParent());
        Files.copy(inputStream, targetPath);

        Document document = new Document();
        document.setOriginalName(originalName);
        document.setStoredName(storedName);
        document.setFilePath(targetPath.toString());
        document.setMimeType(mimeType);
        document.setType(type);
        document.setOwner(owner);
        document.setTransfer(transfer);   // ✅ lien réel avec le transfert
        document.setStatus(DocumentStatus.PENDING);

        return documentDAO.save(document);
    }

    /**
     * Validation KYC par un admin.
     * ✅ NOUVEAU : met à jour le kycStatus du User (DEV-3) si tous ses docs sont validés.
     */
    @Transactional
    public Document validateDocument(Long documentId, String adminComment) {
        Document doc = documentDAO.findById(documentId);
        if (doc == null) throw new IllegalArgumentException("Document introuvable");

        doc.setStatus(DocumentStatus.VALIDATED);
        doc.setValidatedAt(LocalDateTime.now());
        doc.setAdminComment(adminComment);
        Document updated = documentDAO.update(doc);

        // ✅ Vérifier si tous les docs KYC de cet user sont validés
        checkAndUpdateUserKycStatus(doc.getOwner());

        return updated;
    }

    /**
     * Rejet KYC par un admin.
     * ✅ NOUVEAU : repasse le kycStatus du User à REJECTED.
     */
    @Transactional
    public Document rejectDocument(Long documentId, String reason) {
        Document doc = documentDAO.findById(documentId);
        if (doc == null) throw new IllegalArgumentException("Document introuvable");

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setAdminComment(reason);
        Document updated = documentDAO.update(doc);

        // ✅ Mettre à jour le statut KYC de l'utilisateur
        User owner = doc.getOwner();
        if (owner != null) {
            owner.setKycStatus(User.KycStatus.REJECTED);
            userDAO.update(owner);
        }

        return updated;
    }

    public List<Document> getDocumentsByUser(Long userId) {
        return documentDAO.findByOwner(userId);
    }

    public List<Document> getPendingDocuments() {
        return documentDAO.findByStatus(DocumentStatus.PENDING);
    }

    public Document getDocumentById(Long id) {
        return documentDAO.findById(id);
    }

    // ──────────────────────────────────────────────
    // Méthodes privées
    // ──────────────────────────────────────────────

    private boolean isAllowedMimeType(String mimeType) {
        return "application/pdf".equals(mimeType)
                || "image/jpeg".equals(mimeType)
                || "image/png".equals(mimeType);
    }

    private boolean isKycDocument(DocumentType type) {
        return type == DocumentType.CNI_RECTO
                || type == DocumentType.CNI_VERSO
                || type == DocumentType.PASSPORT
                || type == DocumentType.PROOF_OF_ADDRESS;
    }

    private void checkAndUpdateUserKycStatus(User user) {
        if (user == null) return;
        List<Document> docs = documentDAO.findByOwner(user.getId());

        // Au moins un doc KYC doit exister et tous doivent être VALIDATED
        boolean hasKycDoc = docs.stream().anyMatch(d -> isKycDocument(d.getType()));
        boolean allValidated = docs.stream()
                .filter(d -> isKycDocument(d.getType()))
                .allMatch(d -> d.getStatus() == DocumentStatus.VALIDATED);

        if (hasKycDoc && allValidated) {
            user.setKycStatus(User.KycStatus.VERIFIED);
            userDAO.update(user);
        }
    }
}