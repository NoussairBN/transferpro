package ma.transfert.service.doc;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ma.transfert.dao.DocumentDAO;
import ma.transfert.model.Document;
import ma.transfert.model.DocumentType;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Stateless
public class DocumentService {

    private static final String STORAGE_DIR = "uploads/documents/";

    @Inject
    private DocumentDAO documentDAO;

    public Document uploadDocument(InputStream inputStream, String originalName,
                                   String mimeType, DocumentType type, Long ownerId) throws Exception {

        if (!isAllowedMimeType(mimeType)) {
            throw new IllegalArgumentException("File type not allowed");
        }

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
        document.setOwnerId(ownerId);

        return documentDAO.save(document);
    }

    private boolean isAllowedMimeType(String mimeType) {
        return mimeType.equals("application/pdf")
                || mimeType.equals("image/jpeg")
                || mimeType.equals("image/png");
    }
}
