package frauenhaus;

import ch.kova.connector.exception.ComponentObjectModelException;
import ch.kova.connector.exception.ItemNotFoundException;
import ch.kova.connector.exception.LibraryNotFoundException;
import ch.kova.connector.ms.outlook.Outlook;
import ch.kova.connector.ms.outlook.folder.FolderType;
import ch.kova.connector.ms.outlook.folder.OutlookFolder;
import ch.kova.connector.ms.outlook.item.ItemType;
import ch.kova.connector.ms.outlook.mail.OutlookMail;
import compucrash.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportSerienbrief extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CReportSerienbrief.class.getName());
    private final NumberFormat nf = NumberFormat.getInstance();
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportSerienbrief() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    private static String buildSerienbriefSql(StringBuilder verteiler, String foerderverein,
                                              String frauenhaus, boolean eMail) {
        String sql = "SELECT DISTINCT m.* FROM frauenhaus.mitglied m " +
                "WHERE m.mitglied IN (SELECT DISTINCT mitglied " +
                "FROM frauenhaus.stichwort_person WHERE stichwort IN (" + verteiler + ")) ";
        sql += foerderverein + frauenhaus;
        sql += eMail ? " ORDER BY m.email, m.name, m.vorname" : " ORDER BY m.name, m.vorname";
        return sql;
    }

    private static int writeResultRows(ResultSet rset, ResultSetMetaData rsmd, HSSFSheet sheet,
                                       int line, boolean eMail, StringBuilder bcc) throws SQLException {
        while (rset.next()) {
            if (eMail && rset.getString("email") != null) {
                bcc.append(rset.getString("email")).append(";");
            } else {
                HSSFRow row = sheet.createRow(line++);
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    row.createCell((short) i).setCellValue(rset.getString(i + 1));
                }
            }
        }
        return line;
    }

    public Object execute(Object parameters) {

        p = new CProperties();
        p.put("this", this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "SpenderInnen mit einem oder mehreren Stichworten");
        pA.put("height", "150");
        pA.put("multiple", "1");
        pA.put("source", "stichwort");
        pA.put("columns", "1");
        /*		pA = new CProperties();
         p.put(Integer.toString(2),pA);
         pA.put("label", "...aber nicht mit Stichwort");
         pA.put("height", "150");
         pA.put("multiple", "1");
         pA.put("source","stichwort");*/
        pA = new CProperties();
        p.put(Integer.toString(3), pA);
        pA.put("label", "F�rderverein");
        pA.put("check", "1");
        pA = new CProperties();
        p.put(Integer.toString(4), pA);
        pA.put("label", "Frauenhaus");
        pA.put("check", "1");
        pA = new CProperties();
        p.put(Integer.toString(5), pA);
        pA.put("label", "Briefkopf F�rderverein (sonst Frauenhaus)");
        pA.put("check", "1");
//        pA = new CProperties();
//        p.put(Integer.toString(6),pA);
//        pA.put("label", "Email wenn m�glich");
//        pA.put("check", "1");
        new CReportFrame(p);

        return null;
    }

    public void go() {
        StringBuilder verteiler = collectSelectedValues("1");
        String foerderverein = getCheckFilter("3", "AND m.foerderverein = 1 ");
        String frauenhaus = getCheckFilter("4", "AND m.frauenhaus = 1 ");
        String briefkopf = isChecked("5") ? "FVSerienBrief.dot" : "FHSerienBrief.dot";
        boolean eMail = false;
        String betreff = "";
        StringBuilder bcc = new StringBuilder();

        try {
            POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/Serienbrief.xls"));
            HSSFWorkbook wb = new HSSFWorkbook(fsin);
            String SQLString = buildSerienbriefSql(verteiler, foerderverein, frauenhaus, eMail);
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
            HSSFSheet sheet = wb.getSheetAt(0);
            int line = 0;
            ResultSetMetaData rsmd = rset.getMetaData();
            HSSFRow row = sheet.createRow(line++);
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                row.createCell((short) i).setCellValue(rsmd.getColumnLabel(i + 1));
            }
            line = writeResultRows(rset, rsmd, sheet, line, eMail, bcc);
            FileOutputStream fileOut = new FileOutputStream(reports + "\\Serienbrief.xls");
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Serienbrief template not found", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write Serienbrief report", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query Serienbrief data", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            Runtime.getRuntime().exec(excel + "\\winword.exe /z\"" + vorlagen + "\\" + briefkopf + "\"");
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "Failed to launch Word for Serienbrief", e1);
        }
        if (eMail && !bcc.isEmpty()) {
            sendEmail(betreff, bcc.toString(), " ");
        }
    }

    private StringBuilder collectSelectedValues(String key) {
        StringBuilder sb = new StringBuilder();
        if (((CProperties) p.get(key)).get("multipleValue") != null) {
            CTable tab = ((CTable) ((CProperties) p.get(key)).get("multipleValue"));
            int[] rows = tab.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                sb.append("'").append(tab.getValueAt(rows[i], 0).toString().trim()).append("',");
            }
            if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
        }
        return sb;
    }

    private boolean isChecked(String key) {
        Object v = ((CProperties) p.get(key)).get("checkValue");
        return v != null && (Boolean) v;
    }

    private String getCheckFilter(String key, String filter) {
        return isChecked(key) ? filter : "";
    }

    private void sendEmail(String betreff, String bcc, String body) {
        try {
            Outlook outlookApplication = new Outlook();
            OutlookFolder outbox = outlookApplication.getDefaultFolder(FolderType.OUTBOX);
            OutlookMail mail = (OutlookMail) outbox.createItem(ItemType.MAIL);
            mail.setSubject(betreff);
            mail.setTo(CPropertyManager.getInstance().getProperty("email"));
            mail.setBCC(bcc);
            mail.setBody(body);
            mail.display();
            outlookApplication.dispose();
        } catch (ItemNotFoundException ex) {
            LOGGER.log(Level.WARNING, "The outbox folder hasn't been found", ex);
        } catch (ComponentObjectModelException ex) {
            LOGGER.log(Level.SEVERE, "COM error occurred", ex);
        } catch (LibraryNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "The Java Outlook Library has not been found", ex);
        }
    }

    public void set(CProperties p) {
        this.p = p;
    }

}
