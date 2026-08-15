package de.frauenhaus.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests der xlsx-Hilfsfunktionen: Zellentypen, leere Werte und die
 * Serialisierung auch ohne Kopfzeile.
 *
 * @author Robin
 */
class ExcelUtilTest {

    private static Sheet lies(byte[] xlsx, Workbook[] halter) throws IOException
    {
        Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx));
        halter[0] = wb;
        return wb.getSheetAt(0);
    }

    @Nested
    @DisplayName("neuesWorkbook")
    class NeuesWorkbook {

        @Test
        @DisplayName("erzeugt genau ein benanntes Tabellenblatt")
        void neuesWorkbook_erzeugtBenanntesBlatt() throws IOException
        {
            try (Workbook wb = ExcelUtil.neuesWorkbook("Testblatt")) {
                assertThat(wb.getNumberOfSheets()).isEqualTo(1);
                assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Testblatt");
            }
        }
    }

    @Nested
    @DisplayName("zeile")
    class Zeile {

        @Test
        @DisplayName("Zahlen werden numerisch, Datum und Text als Zeichenkette geschrieben")
        void zeile_verschiedeneTypen_werdenKorrektGeschrieben() throws IOException
        {
            Workbook wb = ExcelUtil.neuesWorkbook("Blatt");
            Sheet sheet = wb.getSheetAt(0);
            ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), 0, "A", "B", "C", "D");
            ExcelUtil.zeile(sheet, 1, Arrays.asList(
                    new BigDecimal("12.50"), LocalDate.of(2025, 3, 15), "Text", null));

            Workbook[] halter = new Workbook[1];
            try {
                Row row = lies(ExcelUtil.toBytes(wb), halter).getRow(1);
                assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(12.5);
                assertThat(row.getCell(1).getStringCellValue()).isEqualTo("2025-03-15");
                assertThat(row.getCell(2).getStringCellValue()).isEqualTo("Text");
                assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.BLANK);
            } finally {
                halter[0].close();
            }
        }

        @Test
        @DisplayName("ganze Zahlen bleiben numerisch")
        void zeile_ganzzahl_bleibtNumerisch() throws IOException
        {
            Workbook wb = ExcelUtil.neuesWorkbook("Blatt");
            Sheet sheet = wb.getSheetAt(0);
            ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), 0, "Nr");
            ExcelUtil.zeile(sheet, 1, List.of(42L));

            Workbook[] halter = new Workbook[1];
            try {
                assertThat(lies(ExcelUtil.toBytes(wb), halter).getRow(1).getCell(0).getNumericCellValue())
                        .isEqualTo(42.0);
            } finally {
                halter[0].close();
            }
        }
    }

    @Nested
    @DisplayName("headerZeile")
    class HeaderZeile {

        @Test
        @DisplayName("schreibt alle Titel fett in die angegebene Zeile")
        void headerZeile_schreibtTitelFett() throws IOException
        {
            Workbook wb = ExcelUtil.neuesWorkbook("Blatt");
            ExcelUtil.headerZeile(wb.getSheetAt(0), ExcelUtil.headerStyle(wb), 0, "Eins", "Zwei");

            Workbook[] halter = new Workbook[1];
            try {
                Row kopf = lies(ExcelUtil.toBytes(wb), halter).getRow(0);
                assertThat(kopf.getCell(0).getStringCellValue()).isEqualTo("Eins");
                assertThat(kopf.getCell(1).getStringCellValue()).isEqualTo("Zwei");
                assertThat(halter[0].getFontAt(kopf.getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            } finally {
                halter[0].close();
            }
        }
    }

    @Nested
    @DisplayName("toBytes")
    class ToBytes {

        @Test
        @DisplayName("Blatt ohne Kopfzeile wird ohne Fehler serialisiert")
        void toBytes_ohneKopfzeile_liefertDatei()
        {
            byte[] xlsx = ExcelUtil.toBytes(ExcelUtil.neuesWorkbook("Leer"));

            assertThat(xlsx).isNotEmpty();
            assertThat(new String(xlsx, 0, 2)).isEqualTo("PK");
        }
    }
}
