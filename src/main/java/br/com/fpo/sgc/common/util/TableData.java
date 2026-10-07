package br.com.fpo.sgc.common.util;

import java.util.List;

/** Cabeçalho + linhas de uma tabela de relatório, compartilhado entre {@link CsvWriter} e {@link PdfWriter}. */
public record TableData(List<String> header, List<List<String>> rows) {
}
