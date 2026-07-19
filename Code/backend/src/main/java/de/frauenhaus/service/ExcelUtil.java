package de.frauenhaus.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Nils
 *     <p>Kleine Hilfe für xlsx-Reports (ersetzt die HSSF-/Vorlagen-Mechanik des Altsystems).
 */
final class ExcelUtil {

  private ExcelUtil() {}

  /**
   * @author Nils
   *     <p>Erstellt ein neues Workbook mit genau einem benannten Tabellenblatt.
   */
  static Workbook neuesWorkbook(String sheetName) {
    Workbook wb = new XSSFWorkbook();
    wb.createSheet(sheetName);
    return wb;
  }

  /**
   * @author Nils
   *     <p>Zellenformat für Überschriftenzeilen (fett).
   */
  static CellStyle headerStyle(Workbook wb) {
    CellStyle style = wb.createCellStyle();
    Font font = wb.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  /**
   * @author Nils
   *     <p>Schreibt eine Überschriftenzeile mit den gegebenen Spaltentiteln im übergebenen Stil.
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
   * @author Nils
   *     <p>Schreibt eine Datenzeile. Zahlen werden als Zahl, {@link LocalDate}s als ISO-Text und
   *     alles andere über {@code toString()} geschrieben; {@code null} ergibt eine leere Zelle.
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
   * @author Nils Passt die Spaltenbreiten an den Inhalt der ersten Zeile an und serialisiert das
   *     Workbook.
   */
  static byte[] toBytes(Workbook wb) {
    try (wb;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
