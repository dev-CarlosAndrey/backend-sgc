package br.com.fpo.sgc.common.util;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera um PDF simples (título + tabela) inteiramente em memória — sem gravar em disco, sem
 * {@code File}/{@code Path} em lugar nenhum. O {@code byte[]} devolvido é a única cópia do arquivo; depois
 * de servido na resposta HTTP, não sobra nenhum rastro no backend (decisão de produto: relatório é sempre
 * gerado na hora, nunca armazenado — se precisar de novo, gera de novo).
 */
public final class PdfWriter {

    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Fortaleza"));
    private static final Color HEADER_BACKGROUND = new Color(0x2F, 0x54, 0x96);
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9);

    private PdfWriter() {
    }

    /**
     * @param landscape usar paisagem quando a tabela tiver muitas colunas (ex.: relatório de OS)
     */
    public static byte[] toBytes(String title, List<String> header, List<List<String>> rows, boolean landscape) {
        Document document = new Document(landscape ? PageSize.A4.rotate() : PageSize.A4, 24, 24, 36, 24);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            org.openpdf.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(title, TITLE_FONT));
            document.add(new Paragraph("Gerado em " + GENERATED_AT.format(Instant.now()) + " — SGC", SUBTITLE_FONT));
            document.add(Chunk.NEWLINE);

            if (rows.isEmpty()) {
                document.add(new Paragraph("Nenhum registro encontrado para os filtros aplicados.", CELL_FONT));
            } else {
                document.add(buildTable(header, rows));
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar PDF do relatório", e);
        }

        return out.toByteArray();
    }

    private static PdfPTable buildTable(List<String> header, List<List<String>> rows) {
        PdfPTable table = new PdfPTable(header.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        try {
            table.setWidths(columnWidths(header, rows));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao calcular largura das colunas do PDF", e);
        }

        for (String column : header) {
            PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(column, HEADER_FONT));
            cell.setBackgroundColor(HEADER_BACKGROUND);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (List<String> row : rows) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(value != null ? value : "", CELL_FONT));
                cell.setPadding(4);
                table.addCell(cell);
            }
        }

        return table;
    }

    // Largura proporcional ao maior conteúdo de cada coluna (cabeçalho ou linha), com piso e teto — evita
    // que colunas curtas (ex.: "Status") fiquem tão largas quanto colunas de texto livre (ex.: "Detalhes"),
    // e que uma única célula muito longa domine a tabela inteira.
    private static float[] columnWidths(List<String> header, List<List<String>> rows) {
        float[] widths = new float[header.size()];
        for (int i = 0; i < header.size(); i++) {
            widths[i] = length(header.get(i));
        }
        for (List<String> row : rows) {
            for (int i = 0; i < widths.length && i < row.size(); i++) {
                widths[i] = Math.max(widths[i], length(row.get(i)));
            }
        }
        return widths;
    }

    private static float length(String value) {
        int len = value != null ? value.length() : 0;
        return Math.max(4, Math.min(40, len));
    }
}
