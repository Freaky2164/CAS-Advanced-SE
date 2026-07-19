package de.frauenhaus.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Hilfsfunktionen zum Erzeugen von xlsx-Reports.
 *
 * @author Ole
 */
final class ExcelUtil {

    /** Verhindert Instanziierung der Utility-Klasse. */
    private ExcelUtil() { }

    /**
     * Erstellt ein neues Workbook mit genau einem benannten Tabellenblatt.
     *
     * @param sheetName der Name des Tabellenblatts
     * @return das neue Workbook
     */
    static Workbook neuesWorkbook(String sheetName) {
        Workbook wb = new XSSFWorkbook();
        wb.createSheet(sheetName);
        return wb;
    }

    /**
     * Liefert das Zellenformat für Überschriftenzeilen (fett).
     *
     * @param wb das Workbook, für das der Stil erzeugt wird
     * @return der Überschriften-Stil
     */
    static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Schreibt eine Überschriftenzeile mit den gegebenen Spaltentiteln im
     * übergebenen Stil.
     *
     * @param sheet das Tabellenblatt
     * @param style der Zellen-Stil der Überschriften
     * @param rowNum die Zeilennummer
     * @param titel die Spaltentitel
     */
    static void headerZeile(Sheet sheet, CellStyle style, int rowNum, String... titel) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < titel.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titel[i]);
            cell.setCellStyle(style);
        }
    }

    /**
     * Schreibt eine Datenzeile. Zahlen werden als Zahl, {@link LocalDate}s als
     * ISO-Text und alles andere über {@code toString()} geschrieben;
     * {@code null} ergibt eine leere Zelle.
     *
     * @param sheet das Tabellenblatt
     * @param rowNum die Zeilennummer
     * @param werte die Zellenwerte
     */
    static void zeile(Sheet sheet, int rowNum, List<Object> werte) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < werte.size(); i++) {
            Cell cell = row.createCell(i);
            Object wert = werte.get(i);
            if (wert == null) {
                cell.setBlank();
            } else if (wert instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else if (wert instanceof LocalDate d) {
                cell.setCellValue(d.toString());
            } else {
                cell.setCellValue(wert.toString());
            }
        }
    }

    /**
     * Passt die Spaltenbreiten an den Inhalt an und serialisiert das Workbook.
     *
     * @param wb das zu serialisierende Workbook
     * @return der xlsx-Inhalt als Byte-Array
     */
    static byte[] toBytes(Workbook wb) {
        try (wb; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < wb.getSheetAt(0).getRow(0).getLastCellNum(); i++) {
                wb.getSheetAt(0).autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Excel-Report konnte nicht erzeugt werden", e);
        }
    }
}
