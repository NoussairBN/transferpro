#!/bin/bash

echo "Creating DEV-5 Documents & UI files..."

mkdir -p src/main/java/ma/transfert/model
mkdir -p src/main/java/ma/transfert/dao
mkdir -p src/main/java/ma/transfert/service/doc
mkdir -p src/main/java/ma/transfert/rest
mkdir -p src/main/webapp/resources/css
mkdir -p src/main/webapp/WEB-INF
mkdir -p uploads/documents
mkdir -p uploads/receipts

cat > src/main/java/ma/transfert/model/DocumentType.java <<'EOF'
package ma.transfert.model;

public enum DocumentType {
    CNI_RECTO,
    CNI_VERSO,
    PASSPORT,
    PROOF_OF_ADDRESS,
    RECEIPT,
    CONTRACT,
    REPORT
}
EOF

cat > src/main/java/ma/transfert/model/DocumentStatus.java <<'EOF'
package ma.transfert.model;

public enum DocumentStatus {
    PENDING,
    VALIDATED,
    REJECTED
}
EOF

cat > src/main/java/ma/transfert/model/Document.java <<'EOF'
package ma.transfert.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    private String storedName;
    private String filePath;
    private String mimeType;

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PENDING;

    private Long ownerId;
    private Long transferId;

    private LocalDateTime uploadedAt = LocalDateTime.now();
    private LocalDateTime validatedAt;

    public Long getId() { return id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getTransferId() { return transferId; }
    public void setTransferId(Long transferId) { this.transferId = transferId; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
}
EOF

cat > src/main/java/ma/transfert/dao/DocumentDAO.java <<'EOF'
package ma.transfert.dao;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.transfert.model.Document;
import java.util.List;

@Stateless
public class DocumentDAO {

    @PersistenceContext
    private EntityManager em;

    public Document save(Document document) {
        em.persist(document);
        return document;
    }

    public Document findById(Long id) {
        return em.find(Document.class, id);
    }

    public List<Document> findByOwnerId(Long ownerId) {
        return em.createQuery(
                "SELECT d FROM Document d WHERE d.ownerId = :ownerId",
                Document.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }
}
EOF

cat > src/main/java/ma/transfert/service/doc/DocumentService.java <<'EOF'
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
EOF

cat > src/main/java/ma/transfert/service/doc/PdfReceiptService.java <<'EOF'
package ma.transfert.service.doc;

import jakarta.ejb.Stateless;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Stateless
public class PdfReceiptService {

    public Path generateFakeReceipt() throws Exception {
        Path receiptPath = Paths.get("uploads/receipts/receipt-demo.txt");
        Files.createDirectories(receiptPath.getParent());

        String content = """
                TRANSFERPRO RECEIPT
                -------------------
                Tracking Code: TRF-DEMO-001
                Sender: Demo Sender
                Receiver: Demo Receiver
                Amount: 1500 MAD
                Status: AVAILABLE

                TODO: Replace this text file with real PDF generation using iText/PDFBox.
                """;

        Files.writeString(receiptPath, content);
        return receiptPath;
    }
}
EOF

cat > src/main/java/ma/transfert/rest/DocumentResource.java <<'EOF'
package ma.transfert.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.transfert.service.doc.PdfReceiptService;

@Path("/documents")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    @Inject
    private PdfReceiptService pdfReceiptService;

    @GET
    @Path("/test-receipt")
    public Response testReceipt() {
        try {
            return Response.ok(pdfReceiptService.generateFakeReceipt().toString()).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
EOF

cat > src/main/webapp/login.xhtml <<'EOF'
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<h:head xmlns:h="http://xmlns.jcp.org/jsf/html">
    <title>TransferPro - Login</title>
    <link rel="stylesheet" href="resources/css/style.css"/>
</h:head>
<h:body xmlns:h="http://xmlns.jcp.org/jsf/html">
    <div class="card">
        <h1>TransferPro</h1>
        <p>Connexion</p>
        <input type="text" placeholder="Email"/>
        <input type="password" placeholder="Mot de passe"/>
        <button>Se connecter</button>
    </div>
</h:body>
</html>
EOF

cat > src/main/webapp/dashboard.xhtml <<'EOF'
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<h:head xmlns:h="http://xmlns.jcp.org/jsf/html">
    <title>TransferPro - Dashboard</title>
    <link rel="stylesheet" href="resources/css/style.css"/>
</h:head>
<h:body xmlns:h="http://xmlns.jcp.org/jsf/html">
    <div class="container">
        <h1>Dashboard DEV-5</h1>
        <p>Interface provisoire pour documents, KYC et reçus.</p>
    </div>
</h:body>
</html>
EOF

cat > src/main/webapp/kyc-upload.xhtml <<'EOF'
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<h:head xmlns:h="http://xmlns.jcp.org/jsf/html">
    <title>TransferPro - KYC Upload</title>
    <link rel="stylesheet" href="resources/css/style.css"/>
</h:head>
<h:body xmlns:h="http://xmlns.jcp.org/jsf/html">
    <div class="container">
        <h1>Upload KYC</h1>
        <p>Upload CNI, passeport ou justificatif de domicile.</p>
        <input type="file"/>
        <button>Envoyer</button>
    </div>
</h:body>
</html>
EOF

cat > src/main/webapp/documents.xhtml <<'EOF'
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<h:head xmlns:h="http://xmlns.jcp.org/jsf/html">
    <title>TransferPro - Documents</title>
    <link rel="stylesheet" href="resources/css/style.css"/>
</h:head>
<h:body xmlns:h="http://xmlns.jcp.org/jsf/html">
    <div class="container">
        <h1>Documents</h1>
        <p>Liste provisoire des documents uploadés.</p>
    </div>
</h:body>
</html>
EOF

cat > src/main/webapp/resources/css/style.css <<'EOF'
body {
    margin: 0;
    font-family: Arial, sans-serif;
    background: #0f172a;
    color: #e5e7eb;
}

.container, .card {
    width: 420px;
    margin: 80px auto;
    padding: 30px;
    background: #1e293b;
    border-radius: 18px;
    box-shadow: 0 20px 40px rgba(0,0,0,0.4);
}

h1 {
    color: #38bdf8;
}

input, button {
    display: block;
    width: 100%;
    margin-top: 15px;
    padding: 12px;
    border-radius: 10px;
    border: none;
}

button {
    background: #38bdf8;
    color: #0f172a;
    font-weight: bold;
    cursor: pointer;
}
EOF

cat > src/main/webapp/WEB-INF/beans.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
       bean-discovery-mode="all"
       version="4.0">
</beans>
EOF

echo "DEV-5 files created successfully."
echo "Now update persistence.xml and pom.xml manually."
