package frauenhaus;

import compucrash.*;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportSpendenQuittungen extends CCommand implements CReport {

    private static final String EQUALSVALUE = "equalsValue";
    private static final Logger LOGGER = Logger.getLogger(CReportSpendenQuittungen.class.getName());
    private static final String SPENDEN_QUITTUNG_TEMPLATE = "/SpendenQuittungen.xls";
    private static final String SPENDEN_QUITTUNG_PREFIX = "\\SpendenQuittung";
    private final NumberFormat nf = NumberFormat.getInstance();
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private CProperties p;
    private String jahr = "";

    public CReportSpendenQuittungen() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
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
        // Jahr setzen und Reports aufrufen        
        if (((CProperties) p.get("1")).get(EQUALSVALUE) != null && !((CProperties) p.get("1")).get(EQUALSVALUE).toString().trim().equalsIgnoreCase("")) {
            jahr = ((CProperties) p.get("1")).get(EQUALSVALUE).toString().trim();
            }

        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String sql = "SELECT distinct s.verein, a.spendentyp " +
                "FROM frauenhaus.spende s, frauenhaus.spendenart a " +
                "WHERE a.spendenart = s.spendenart " +
                "AND s.datum = ? " +
                "AND a.spendentyp IN ('Geldspende dauer', 'Mitgliedsbeitrag') " +
                "ORDER BY s.verein, a.spendentyp";
        CMessage.print(sql);
        try (PreparedStatement stmt = CDataManager.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, jahr);
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    String verein = rset.getString(1);
                    String spendentyp = rset.getString(2);
                    report(verein, spendentyp);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    private void report(String verein, String spendentyp) {
        CMessage.print("Report: " + verein + " " + spendentyp);
        String sql = "SELECT DISTINCT m.mitglied, m.anrede, m.vorname, m.name, m.name2, m.name3, m.strasse, m.plz, m.ort " +
                "FROM frauenhaus.mitglied m " +
                "WHERE EXISTS (SELECT * " +
                "FROM frauenhaus.spende s, frauenhaus.spendenart a " +
                "WHERE s.mitglied = m.mitglied " +
                "AND s.spendenart = a.spendenart " +
                "AND YEAR(s.datum) = ? " +
                "AND a.spendentyp = ? " +
                "AND s.verein = ?)";
        try (POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + SPENDEN_QUITTUNG_TEMPLATE));
             HSSFWorkbook wb = new HSSFWorkbook(fsin);
             PreparedStatement stmt = CDataManager.getInstance().getConnection().prepareStatement(sql)) {
            stmt.setString(1, jahr);
            stmt.setString(2, spendentyp);
            stmt.setString(3, verein);
            try (ResultSet rset = stmt.executeQuery()) {
                HSSFSheet sheet = wb.getSheetAt(0);
                int line = 1;
                String amountSql = "SELECT s.datum, s.betrag, coalesce(s.bemerkung, ' ') " +
                        "FROM frauenhaus.spende s, frauenhaus.spendenart a " +
                        "WHERE s.mitglied = ? " +
                        "AND YEAR(s.datum) = ? " +
                        "AND s.spendenart = a.spendenart " +
                        "AND a.spendentyp = ? " +
                        "AND verein = ? " +
                        "ORDER BY s.datum";
                try (PreparedStatement amountStmt = CDataManager.getInstance().getConnection().prepareStatement(amountSql)) {
                    amountStmt.setString(2, jahr);
                    amountStmt.setString(3, spendentyp);
                    amountStmt.setString(4, verein);
                    while (rset.next()) {
                        HSSFRow row = sheet.createRow(line++);
                        String mitglied = rset.getString(1);
                        setCellValue(row, 0, rset.getString(2));
                        setCellValue(row, 1, rset.getString(3));
                        setCellValue(row, 2, rset.getString(4));
                        setCellValue(row, 3, rset.getString(5));
                        setCellValue(row, 4, rset.getString(6));
                        setCellValue(row, 5, rset.getString(7));
                        setCellValue(row, 6, rset.getString(8));
                        setCellValue(row, 7, rset.getString(9));
                        setCellValue(row, 8, verein);
                        setCellValue(row, 9, spendentyp);
                        setCellValue(row, 10, jahr);

                        amountStmt.setString(1, mitglied);
                        try (ResultSet rset0 = amountStmt.executeQuery()) {
                            double betrag = 0;
                            StringBuilder betraege = new StringBuilder();
                            StringBuilder bemerkung = new StringBuilder();
                            while (rset0.next()) {
                                LocalDate datum = rset0.getDate(1).toLocalDate();
                                double einzelbetrag = rset0.getDouble(2);
                                betraege.append(fmt.format(datum)).append('\t').append(nf.format(einzelbetrag)).append('\n');
                                betrag += einzelbetrag;
                                String temp = rset0.getString(3).trim();
                                if (!temp.isEmpty()) {
                                    bemerkung.append(temp).append('\n');
                                }
                            }
                            trimTrailingNewline(betraege);
                            trimTrailingNewline(bemerkung);
                            setCellValue(row, 11, nf.format(betrag));
                            setCellValue(row, 12, betragInWorten(nf.format(betrag)));
                            setCellValue(row, 13, betraege.toString());
                            setCellValue(row, 14, bemerkung.toString());
                        }
                    }
                }
            }
            try (FileOutputStream fileOut = new FileOutputStream(reports + SPENDEN_QUITTUNG_PREFIX + verein + spendentyp + ".xls")) {
                wb.write(fileOut);
            }
            new ProcessBuilder(
                    excel + "\\winword.exe",
                    "/z" + vorlagen + SPENDEN_QUITTUNG_PREFIX + verein + spendentyp + ".dot"
            ).start();
            CMessage.print(excel + "\\winword.exe /z\"" + vorlagen + SPENDEN_QUITTUNG_PREFIX + verein + spendentyp + ".dot\"");

        } catch (IOException | SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private String betragInWorten(String zahl) {
        char[] ziffer = zahl.toCharArray();
        StringBuilder wert = new StringBuilder();
        for (int i = 0; i < ziffer.length; i++) {
            switch (ziffer[i]) {
                case '1':
                    wert.append("Eins - ");
                    break;
                case '2':
                    wert.append("Zwei - ");
                    break;
                case '3':
                    wert.append("Drei - ");
                    break;
                case '4':
                    wert.append("Vier - ");
                    break;
                case '5':
                    wert.append("F�nf - ");
                    break;
                case '6':
                    wert.append("Sechs - ");
                    break;
                case '7':
                    wert.append("Sieben - ");
                    break;
                case '8':
                    wert.append("Acht - ");
                    break;
                case '9':
                    wert.append("Neun - ");
                    break;
                case '0':
                    wert.append("Null - ");
                    break;
                case ',':
                    wert.append("Komma - ");
                    break;
                default:
                    break;
            }
        }
        if (wert.length() >= 3) {
            wert.setLength(wert.length() - 3);
        }
        return wert.toString();
    }

    public void set(CProperties p) {
        this.p = p;
    }

    private void setCellValue(HSSFRow row, int index, String value) {
        HSSFCell cell = row.createCell((short) index);
        cell.setCellValue(value);
    }

    private void trimTrailingNewline(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.setLength(builder.length() - 1);
        }
    }

}
