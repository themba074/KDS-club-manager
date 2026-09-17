package com.kds.backend.reports.infrastructure;

import com.kds.backend.reports.application.ReportSnapshot;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class ReportPdfExporter {
    private static final float MARGIN = 42;
    private static final float LINE = 15;

    public void write(ReportSnapshot snapshot, OutputStream output) throws IOException {
        try (var document = new PDDocument()) {
            var regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            int index = 0;
            do {
                var page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                float y = page.getMediaBox().getHeight() - MARGIN;
                try (var content = new PDPageContentStream(document, page)) {
                    draw(content, bold, 16, y, snapshot.title());
                    y -= 23;
                    draw(content, regular, 9, y, "Generated " + snapshot.generatedAt());
                    y -= 26;
                    if (snapshot.rows().isEmpty()) draw(content, regular, 10, y, "No records for this report.");
                    while (index < snapshot.rows().size() && y > MARGIN + snapshot.headers().size() * LINE + 20) {
                        var row = snapshot.rows().get(index++);
                        for (int column = 0; column < snapshot.headers().size(); column++) {
                            draw(content, regular, 9, y, snapshot.headers().get(column) + ": " + row.get(column));
                            y -= LINE;
                        }
                        y -= 10;
                    }
                }
            } while (index < snapshot.rows().size());
            document.save(output);
        }
    }

    private static void draw(PDPageContentStream content, PDType1Font font, int size, float y, String value) throws IOException {
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String character = String.valueOf(value.charAt(i));
            try { font.encode(character); safe.append(character); }
            catch (IllegalArgumentException exception) { safe.append('?'); }
        }
        String text = safe.length() > 105 ? safe.substring(0, 102) + "..." : safe.toString();
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(MARGIN, y);
        content.showText(text);
        content.endText();
    }
}
