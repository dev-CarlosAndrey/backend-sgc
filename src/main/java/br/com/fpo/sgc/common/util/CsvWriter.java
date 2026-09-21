package br.com.fpo.sgc.common.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gera CSV pensado para abrir direto no Excel em pt-BR: separador ";", UTF-8 com BOM e quebra CRLF.
 * Células que começam com =, +, - ou @ recebem um apóstrofo, evitando que texto digitado por usuários
 * seja interpretado como fórmula (CSV injection).
 */
public final class CsvWriter {

    private static final char SEPARATOR = ';';
    private static final String NEW_LINE = "\r\n";
    private static final String BOM = "﻿";

    private CsvWriter() {
    }

    public static byte[] toBytes(List<String> header, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder(BOM);
        appendRow(csv, header);
        rows.forEach(row -> appendRow(csv, row));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(escape(cells.get(i)));
        }
        csv.append(NEW_LINE);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String cell = value;
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) {
            cell = "'" + cell;
        }
        if (cell.indexOf(SEPARATOR) >= 0 || cell.indexOf('"') >= 0 || cell.indexOf('\n') >= 0 || cell.indexOf('\r') >= 0) {
            cell = "\"" + cell.replace("\"", "\"\"") + "\"";
        }
        return cell;
    }
}
