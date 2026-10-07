package br.com.fpo.sgc.common.util;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
    private static final float PAGE_MARGIN_LEFT_RIGHT = 24f;
    private static final float HEADER_CELL_PADDING = 5f;
    private static final float BODY_CELL_PADDING = 4f;
    private static final float WIDTH_SAFETY_BUFFER = 2f;
    private static final float MAX_COLUMN_WIDTH = 170f;

    // Cabeçalhos longos usados pelos relatórios (os mesmos do CSV, onde o nome completo é melhor — o Excel
    // ajusta a largura da coluna sozinho) ficam abreviados só aqui no PDF, onde a largura da coluna é fixa
    // e o texto do cabeçalho pode quebrar no meio da palavra se não couber numa linha.
    private static final Map<String, String> HEADER_ABBREVIATIONS = Map.ofEntries(
            Map.entry("Categoria", "Categ."),
            Map.entry("Código", "Cód."),
            Map.entry("Prioridade", "Prior."),
            Map.entry("Solicitante", "Solic."),
            Map.entry("Técnico", "Téc."),
            Map.entry("Bem (tombamento)", "Bem (tomb.)"),
            Map.entry("Tombamento", "Tomb."),
            Map.entry("Valor de aquisição", "Val. aquis."),
            Map.entry("Custo de manutenção", "Custo manut."),
            Map.entry("Custo / valor (%)", "Custo/valor (%)"),
            Map.entry("Candidato a substituição", "Candid. subst."),
            Map.entry("Setor de origem", "Setor origem"),
            Map.entry("Setor de destino", "Setor destino"),
            Map.entry("Custo das concluídas", "Custo concl."),
            Map.entry("Média (h) / 1º atendimento (h)", "Média/1º atend. (h)"),
            Map.entry("Máx. (h) / Resolução média (h)", "Máx./Resol. (h)"),
            Map.entry("Estado de conservação", "Estado conserv."),
            Map.entry("Sem valorização", "Sem valoriz."),
            Map.entry("Depreciação acumulada", "Deprec. acum."),
            Map.entry("Valor contábil", "Val. contábil"),
            Map.entry("Disponível", "Dispon."),
            Map.entry("Em manutenção", "Em manut.")
    );

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
                List<String> abbreviatedHeader = header.stream().map(PdfWriter::abbreviate).toList();
                float pageWidth = landscape ? PageSize.A4.rotate().getWidth() : PageSize.A4.getWidth();
                float availableWidth = pageWidth - (2 * PAGE_MARGIN_LEFT_RIGHT);
                document.add(buildTable(abbreviatedHeader, rows, availableWidth));
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar PDF do relatório", e);
        }

        return out.toByteArray();
    }

    private static PdfPTable buildTable(List<String> header, List<List<String>> rows, float availableWidth) {
        PdfPTable table = new PdfPTable(header.size());
        table.setHeaderRows(1);
        float[] widths = columnWidths(header, rows, availableWidth);
        try {
            table.setTotalWidth(widths);
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao calcular largura das colunas do PDF", e);
        }
        table.setLockedWidth(true);

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

    // Largura em pontos medida com a métrica real da fonte (não contagem de caracteres) — garante que o
    // cabeçalho (já abreviado) sempre caiba em uma linha. Cada coluna recebe no mínimo a largura do seu
    // próprio cabeçalho (minWidths); o espaço excedente (naturalWidths - minWidths) de colunas de texto
    // livre (ex.: "Título", "Setor") é o único reduzido quando a soma ultrapassa a largura da página —
    // nunca o espaço reservado ao cabeçalho, que não tem folga pra ceder.
    private static float[] columnWidths(List<String> header, List<List<String>> rows, float availableWidth) {
        BaseFont headerBaseFont = HEADER_FONT.getCalculatedBaseFont(false);
        BaseFont cellBaseFont = CELL_FONT.getCalculatedBaseFont(false);
        int columnCount = header.size();

        float[] minWidths = new float[columnCount];
        float[] naturalWidths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            float headerWidth = headerBaseFont.getWidthPoint(header.get(i), HEADER_FONT.getSize())
                    + (2 * HEADER_CELL_PADDING) + WIDTH_SAFETY_BUFFER;
            minWidths[i] = headerWidth;
            naturalWidths[i] = headerWidth;
        }
        for (List<String> row : rows) {
            for (int i = 0; i < columnCount && i < row.size(); i++) {
                String value = row.get(i) != null ? row.get(i) : "";
                float cellWidth = cellBaseFont.getWidthPoint(value, CELL_FONT.getSize())
                        + (2 * BODY_CELL_PADDING) + WIDTH_SAFETY_BUFFER;
                naturalWidths[i] = Math.max(naturalWidths[i], Math.min(cellWidth, MAX_COLUMN_WIDTH));
            }
        }

        float totalNatural = 0;
        for (float w : naturalWidths) {
            totalNatural += w;
        }
        if (totalNatural <= availableWidth) {
            return naturalWidths;
        }

        float totalSlack = 0;
        for (int i = 0; i < columnCount; i++) {
            totalSlack += naturalWidths[i] - minWidths[i];
        }
        if (totalSlack <= 0) {
            return minWidths;
        }

        float excess = totalNatural - availableWidth;
        float shrinkRatio = Math.min(1f, excess / totalSlack);
        float[] widths = new float[columnCount];
        for (int i = 0; i < columnCount; i++) {
            float slack = naturalWidths[i] - minWidths[i];
            widths[i] = naturalWidths[i] - (slack * shrinkRatio);
        }
        return widths;
    }

    private static String abbreviate(String header) {
        return HEADER_ABBREVIATIONS.getOrDefault(header, header);
    }
}
