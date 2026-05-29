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
