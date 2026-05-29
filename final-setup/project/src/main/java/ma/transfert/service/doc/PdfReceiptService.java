package ma.transfert.service.doc;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import ma.transfert.dao.TransferDAO;
import ma.transfert.model.Transfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Stateless
public class PdfReceiptService {

    private static final String RECEIPTS_DIR = "uploads/receipts/";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ✅ NOUVEAU : injecte le vrai TransferDAO de DEV-2
    @EJB
    private TransferDAO transferDAO;

    /**
     * Génère un reçu PDF pour un transfert réel.
     * Remplace complètement l'ancienne méthode generateFakeReceipt().
     *
     * @param transferId ID du transfert (DEV-2)
     * @return Path vers le fichier PDF généré
     */
    public Path generateReceiptForTransfer(Long transferId) throws Exception {
        Transfer transfer = transferDAO.findById(transferId);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfert introuvable : " + transferId);
        }
        return generatePdf(transfer);
    }

    /**
     * Génère un reçu PDF par code de suivi.
     */
    public Path generateReceiptByTrackingCode(String trackingCode) throws Exception {
        Transfer transfer = transferDAO.findByTrackingCode(trackingCode);
        if (transfer == null) {
            throw new IllegalArgumentException("Transfert introuvable : " + trackingCode);
        }
        return generatePdf(transfer);
    }

    // ──────────────────────────────────────────────
    // Génération PDF avec iText 7
    // ──────────────────────────────────────────────

    public Path generatePdf(Transfer transfer) throws Exception {
        Files.createDirectories(Paths.get(RECEIPTS_DIR));

        String fileName = "receipt-" + transfer.getTrackingCode() + ".pdf";
        Path outputPath = Paths.get(RECEIPTS_DIR, fileName);

        try (PdfWriter writer = new PdfWriter(outputPath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            PdfFont bold = loadBoldFont();
            PdfFont regular = loadRegularFont();

            // ── En-tête ──
            doc.add(new Paragraph("TRANSFERPRO")
                    .setFont(bold)
                    .setFontSize(22)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY));

            doc.add(new Paragraph("Reçu de Transfert")
                    .setFont(regular)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // ── Tableau des données ──
            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .useAllAvailableWidth()
                    .setMarginTop(10);

            addRow(table, "Code de suivi", transfer.getTrackingCode(), bold, regular);
            addRow(table, "Statut", transfer.getStatus().name(), bold, regular);
            addRow(table, "Expéditeur", transfer.getSenderName(), bold, regular);
            addRow(table, "Téléphone expéditeur",
                    transfer.getSenderPhone() != null ? transfer.getSenderPhone() : "-", bold, regular);
            addRow(table, "Bénéficiaire", transfer.getReceiverName(), bold, regular);
            addRow(table, "Téléphone bénéficiaire", transfer.getReceiverPhone(), bold, regular);
            addRow(table, "Montant", transfer.getAmount() + " MAD", bold, regular);
            addRow(table, "Frais", transfer.getFees() + " MAD", bold, regular);
            addRow(table, "Total", transfer.getTotalAmount() + " MAD", bold, regular);
            addRow(table, "Date de création",
                    transfer.getCreatedAt() != null
                            ? transfer.getCreatedAt().format(FORMATTER) : "-", bold, regular);

            if (transfer.getPaidAt() != null) {
                addRow(table, "Date de paiement",
                        transfer.getPaidAt().format(FORMATTER), bold, regular);
            }

            if (transfer.getSendingAgency() != null) {
                addRow(table, "Agence émettrice",
                        transfer.getSendingAgency().getName(), bold, regular);
            }

            doc.add(table);

            // ── Pied de page ──
            doc.add(new Paragraph("\nDocument généré automatiquement par TransferPro")
                    .setFont(regular)
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginTop(30));
        }

        return outputPath;
    }

    private void addRow(Table table, String label, String value,
                        PdfFont bold, PdfFont regular) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(bold).setFontSize(11))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(6));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "-").setFont(regular).setFontSize(11))
                .setPadding(6));
    }

    private PdfFont loadBoldFont() {
        try {
            return PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la police Bold", e);
        }
    }

    private PdfFont loadRegularFont() {
        try {
            return PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.HELVETICA);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger la police Regular", e);
        }
    }
}