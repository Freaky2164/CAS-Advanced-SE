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

public class CCommandSpendenQuittung extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CCommandSpendenQuittung.class.getName());
    private final NumberFormat nf = NumberFormat.getInstance();
    private final String reports;
    private final String vorlagen;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private String spende;
    private String verein;
    private String spendenart;
    private String datum;

    public CCommandSpendenQuittung() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
    }

    public Object execute(Object parameters) {
        if (getOwner() instanceof CInfoFrame frame) {
            Object o = frame.getAttributeValue("frauenhaus.spende.spende");
            spende = o.toString();
            o = frame.getAttributeValue("frauenhaus.spende.verein");
            verein = o.toString();

            o = frame.getAttributeValue("frauenhaus.spende.spendenart");
            spendenart = o.toString();
            o = frame.getAttributeValue("frauenhaus.spende.datum");
            datum = o.toString();
        }
        go();
        return null;
    }

    public void go() {
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        // Spendentyp aus Spendenart
        String sqlString = "SELECT spendentyp FROM frauenhaus.spendenart WHERE spendenart = ? ORDER BY spendentyp";
        try (PreparedStatement stmt = CDataManager.getInstance().getConnection().prepareStatement(sqlString)) {
            stmt.setString(1, spendenart);
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    report(verein, rset.getString(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load spendentyp", e);
        }
    }

    private void report(String verein, String spendentyp) {
        LOGGER.log(Level.INFO, "Report: {0} {1}", new Object[]{verein, spendentyp});
        String sqlString = "SELECT DISTINCT m.mitglied, m.anrede, m.vorname, m.name, m.name2, m.name3, m.strasse, m.plz, m.ort "
                + "FROM frauenhaus.mitglied m "
                + "WHERE EXISTS (SELECT * "
                + "FROM frauenhaus.spende s "
                + "WHERE s.mitglied = m.mitglied "
                + "AND s.spende = ?)";
        try (POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/SpendenQuittungen.xls"));
             HSSFWorkbook wb = new HSSFWorkbook(fsin);
             PreparedStatement stmt = CDataManager.getInstance().getConnection().prepareStatement(sqlString)) {
            stmt.setString(1, spende);
            try (ResultSet rset = stmt.executeQuery()) {
                HSSFSheet sheet = wb.getSheetAt(0);
                int line = 1;
                while (rset.next()) {
                    HSSFRow row = sheet.createRow(line++);
                    String mitglied = rset.getString(1);
                    HSSFCell cell = row.createCell((short) 0);
                    cell.setCellValue(rset.getString(2));
                    cell = row.createCell((short) 1);
                    cell.setCellValue(rset.getString(3));
                    cell = row.createCell((short) 2);
                    cell.setCellValue(rset.getString(4));
                    cell = row.createCell((short) 3);
                    cell.setCellValue(rset.getString(5));
                    cell = row.createCell((short) 4);
                    cell.setCellValue(rset.getString(6));
                    cell = row.createCell((short) 5);
                    cell.setCellValue(rset.getString(7));
                    cell = row.createCell((short) 6);
                    cell.setCellValue(rset.getString(8));
                    cell = row.createCell((short) 7);
                    cell.setCellValue(rset.getString(9));
                    cell = row.createCell((short) 8);
                    cell.setCellValue(verein);
                    cell = row.createCell((short) 9);
                    cell.setCellValue(spendentyp);
                    cell = row.createCell((short) 10);
                    cell.setCellValue(datum);
                    fillDonationSummary(row, mitglied, verein, spendentyp, datum);
                }
            }
            try (FileOutputStream fileOut = new FileOutputStream(reports + "\\SpendenQuittung" + verein + spendentyp + ".xls")) {
                wb.write(fileOut);
            }
            LOGGER.log(Level.INFO, "Report generated: {0}\\SpendenQuittung{1}{2}.xls", new Object[]{reports, verein, spendentyp});

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write donation receipt workbook", e);
            Toolkit.getDefaultToolkit().beep();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to build donation receipt", e);
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void fillDonationSummary(HSSFRow row, String mitglied, String verein, String spendentyp, String datum)
            throws SQLException {
        String sqlString;
        if (spendentyp.equalsIgnoreCase("Dauerspende")) {
            sqlString = "SELECT s.datum, s.betrag, s.bemerkung "
                    + "FROM frauenhaus.spende s, frauenhaus.spendenart a "
                    + "WHERE s.mitglied = ? "
                    + "AND YEAR(s.datum) = YEAR(?) "
                    + "AND a.spendenart = s.spendenart "
                    + "AND a.spendentyp = ? "
                    + "AND s.verein = ? "
                    + "ORDER BY datum";
        } else {
            sqlString = "SELECT datum, betrag, coalesce(bemerkung, ' ') "
                    + "FROM frauenhaus.spende s "
                    + "WHERE s.spende = ? "
                    + "ORDER BY datum";
        }
        try (PreparedStatement stmt0 = CDataManager.getInstance().getConnection().prepareStatement(sqlString)) {
            if (spendentyp.equalsIgnoreCase("Dauerspende")) {
                stmt0.setString(1, mitglied);
                stmt0.setString(2, datum);
                stmt0.setString(3, spendentyp);
                stmt0.setString(4, verein);
            } else {
                stmt0.setString(1, spende);
            }
            try (ResultSet rset0 = stmt0.executeQuery()) {
                double betrag = 0;
                StringBuilder betraege = new StringBuilder();
                StringBuilder bemerkung = new StringBuilder();
                while (rset0.next()) {
                    LocalDate entryDate = rset0.getDate(1).toLocalDate();
                    double einzelbetrag = rset0.getDouble(2);
                    betraege.append(fmt.format(entryDate)).append('\t').append(nf.format(einzelbetrag)).append('\n');
                    betrag += einzelbetrag;
                    String temp = rset0.getString(3).trim();
                    if (!temp.isEmpty()) {
                        bemerkung.append(temp).append('\n');
                    }
                }
                if (!betraege.isEmpty()) {
                    betraege.setLength(betraege.length() - 1);
                }
                if (!bemerkung.isEmpty()) {
                    bemerkung.setLength(bemerkung.length() - 1);
                }
                HSSFCell cell = row.createCell((short) 11);
                cell.setCellValue(nf.format(betrag));
                cell = row.createCell((short) 12);
                cell.setCellValue(betragInWorten(nf.format(betrag)));
                cell = row.createCell((short) 13);
                cell.setCellValue(betraege.toString());
                cell = row.createCell((short) 14);
                cell.setCellValue(bemerkung.toString());
            }
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
        // no-op
    }

}
