package frauenhaus;

import compucrash.*;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportSpendenUebersicht extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CReportSpendenUebersicht.class.getName());
    private final NumberFormat nf = NumberFormat.getInstance();
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportSpendenUebersicht() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    private static int writeSummaryRow(HSSFSheet sheet, int line, String label, double value,
                                       HSSFCellStyle titleStyle, HSSFCellStyle moneyStyle) {
        HSSFRow row = sheet.createRow(line++);
        HSSFCell c0 = row.createCell((short) 0);
        c0.setCellValue(label);
        c0.setCellStyle(titleStyle);
        row.createCell((short) 1).setCellStyle(titleStyle);
        row.createCell((short) 2).setCellStyle(titleStyle);
        HSSFCell c3 = row.createCell((short) 3);
        c3.setCellStyle(moneyStyle);
        c3.setCellValue(value);
        return line;
    }

    private static void writeResultToSheet(ResultSet rset, HSSFSheet sheet, SpendenStyles styles) throws SQLException {
        int line = -1;
        String verein = "init";
        String spendentyp = "init";
        String spendenart = "init";
        double sum = 0;
        double summeGesamt = 0;
        boolean sumLine = false;
        while (rset.next()) {
            if (!rset.getString(1).equalsIgnoreCase(verein)) {
                if (!spendentyp.equalsIgnoreCase("init")) {
                    line = writeSummaryRow(sheet, line, spendentyp + " Gesamt:", sum, styles.gelbTitelStyle, styles.gelbGeldStyle);
                    sum = 0;
                    sumLine = true;
                    line = writeSummaryRow(sheet, line, "Gesamt:", summeGesamt, styles.gelbTitelStyle, styles.gelbGeldStyle);
                    summeGesamt = 0;
                }
                verein = rset.getString(1);
                sheet.createRow(line++);
                line = writeSummaryRow(sheet, line, verein, -1, styles.gelbTitelStyle, styles.gelbTitelStyle);
            }
            if (!rset.getString(2).equalsIgnoreCase(spendentyp)) {
                if (!sumLine && !spendentyp.equalsIgnoreCase("init")) {
                    line = writeSummaryRow(sheet, line, spendentyp + " Gesamt:", sum, styles.gelbTitelStyle, styles.gelbGeldStyle);
                    sum = 0;
                }
                sumLine = false;
                sheet.createRow(line++);
                spendentyp = rset.getString(2);
                HSSFRow hdr = sheet.createRow(line++);
                HSSFCell c = hdr.createCell((short) 0);
                c.setCellValue(spendentyp);
                c.setCellStyle(styles.gelbTitelStyle);
                hdr.createCell((short) 1).setCellStyle(styles.gelbTitelStyle);
            }
            if (!rset.getString(3).equalsIgnoreCase(spendenart)) {
                spendenart = rset.getString(3);
                HSSFRow artRow = sheet.createRow(line++);
                HSSFCell c = artRow.createCell((short) 0);
                c.setCellValue(spendenart);
                c.setCellStyle(styles.gelbTitelStyle);
            }
            HSSFRow dataRow = sheet.createRow(line++);
            dataRow.createCell((short) 0).setCellValue(rset.getString(4));
            dataRow.createCell((short) 1).setCellValue(rset.getString(5));
            HSSFCell dateCell = dataRow.createCell((short) 2);
            dateCell.setCellValue(rset.getDate(6));
            dateCell.setCellStyle(styles.datumStyle);
            HSSFCell moneyCell = dataRow.createCell((short) 3);
            moneyCell.setCellValue(rset.getDouble(7));
            moneyCell.setCellStyle(styles.geldStyle);
            sum += rset.getDouble(7);
            summeGesamt += rset.getDouble(7);
        }
        writeSummaryRow(sheet, line++, spendentyp + " Gesamt:", sum, styles.gelbTitelStyle, styles.gelbGeldStyle);
        writeSummaryRow(sheet, line, "Gesamt:", summeGesamt, styles.gelbTitelStyle, styles.gelbGeldStyle);
    }

    public Object execute(Object parameters) {

        p = new CProperties();
        p.put("this", this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "Jahr");
        pA.put("equals", "1");
        new CReportFrame(p);

        return null;
    }

    public void go() {
        String jahr = getJahrFilter();
        try {
            POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/SpendenUebersicht.xls"));
            HSSFWorkbook wb = new HSSFWorkbook(fsin);
            String SQLString = "SELECT s.verein, a.spendentyp, s.spendenart, m.name, m.vorname, s.datum, s.betrag " +
                    "FROM frauenhaus.mitglied m, frauenhaus.spende s, frauenhaus.spendenart a " +
                    "WHERE s.mitglied = m.mitglied AND s.spendenart = a.spendenart " +
                    jahr + " ORDER BY s.verein, a.spendentyp, s.spendenart, m.name, m.vorname, s.datum ";
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
            HSSFSheet sheet = wb.getSheetAt(0);
            SpendenStyles styles = new SpendenStyles(sheet);
            writeResultToSheet(rset, sheet, styles);
            FileOutputStream fileOut = new FileOutputStream(reports + "\\SpendenUebersicht.xls");
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Report template file not found", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write SpendenUebersicht report", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query SpendenUebersicht data", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            Runtime.getRuntime().exec(excel + "\\excel.exe " + reports + "\\SpendenUebersicht.xls");
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "Failed to open SpendenUebersicht in Excel", e1);
        }
    }

    private String getJahrFilter() {
        Object v = ((CProperties) p.get("1")).get("equalsValue");
        if (v != null && !v.toString().trim().equalsIgnoreCase("")) {
            return " AND year(s.datum) = '" + v.toString().trim() + "' ";
        }
        return "";
    }

    public void set(CProperties p) {
        this.p = p;
    }

    private static class SpendenStyles {
        final HSSFCellStyle gelbTitelStyle;
        final HSSFCellStyle gelbGeldStyle;
        final HSSFCellStyle datumStyle;
        final HSSFCellStyle geldStyle;

        SpendenStyles(HSSFSheet sheet) {
            gelbTitelStyle = sheet.getRow(0).getCell((short) 0).getCellStyle();
            gelbGeldStyle = sheet.getRow(0).getCell((short) 3).getCellStyle();
            datumStyle = sheet.getRow(1).getCell((short) 2).getCellStyle();
            geldStyle = sheet.getRow(1).getCell((short) 3).getCellStyle();
        }
    }

}
