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
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportVerteiler extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CReportVerteiler.class.getName());
    private final NumberFormat nf = NumberFormat.getInstance();
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportVerteiler() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    private static int writeVerteilerRow(ResultSet rset, HSSFSheet sheet, int line,
                                         String betreff, String text) throws SQLException {
        HSSFRow row = sheet.createRow(line++);
        row.createCell((short) 0).setCellValue(rset.getString(1));
        row.createCell((short) 1).setCellValue(rset.getString(2));
        row.createCell((short) 2).setCellValue(rset.getString(3));
        row.createCell((short) 3).setCellValue(rset.getString(4));
        row.createCell((short) 4).setCellValue(rset.getString(5));
        row.createCell((short) 5).setCellValue(rset.getString(6));
        row.createCell((short) 6).setCellValue(rset.getString(8));
        row.createCell((short) 7).setCellValue(rset.getString(9));
        row.createCell((short) 8).setCellValue(rset.getString(10));
        row.createCell((short) 9).setCellValue(betreff);
        row.createCell((short) 10).setCellValue(text);
        return line;
    }

    public Object execute(Object parameters) {

        p = new CProperties();
        p.put("this", this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "Adressen nach Stichwort...");
        pA.put("height", "150");
        pA.put("multiple", "1");
        pA.put("source", "stichwort");
//		pA = new CProperties();
//		p.put(Integer.toString(2),pA);
//		pA.put("label", "...aber nicht aus Verteiler");
//		pA.put("height", "150");
//		pA.put("multiple", "1");
//		pA.put("source","verteiler");
//		pA = new CProperties();
//		p.put(Integer.toString(3),pA);
//		pA.put("label", "Kontaktdatum");
//		pA.put("between", "1");
        pA = new CProperties();
        p.put(Integer.toString(3), pA);
        pA.put("label", "Email-Betreff");
        pA.put("equals", "1");
        pA = new CProperties();
        p.put(Integer.toString(4), pA);
        pA.put("label", "Email-Text");
        pA.put("text", "1");
        pA = new CProperties();
        p.put(Integer.toString(5), pA);
        pA.put("label", "Email-Anhang");
        pA.put("file", "1");
        new CReportFrame(p);

//		go();
        return null;
    }

    public void go() {
        StringBuilder verteiler = collectMultipleValues("1");
        String betreff = getStringValue("3", "equalsValue");
        String text = getStringValue("4", "textValue");
        String anhang = getStringValue("5", "fileValue");
        StringBuilder bcc = new StringBuilder();

        try {
            POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/Verteiler.xls"));
            HSSFWorkbook wb = new HSSFWorkbook(fsin);
            String SQLString = "SELECT DISTINCT k.anrede, k.vorname, k.name, k.strasse, k.plz, k.ort, k.email, name2, name3, briefanrede " +
                    "FROM frauenhaus.mitglied k " +
                    "WHERE k.mitglied IN (SELECT mitglied " +
                    "FROM frauenhaus.stichwort_person WHERE stichwort IN (" + verteiler + ")) ORDER BY k.email";
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
            HSSFSheet sheet = wb.getSheetAt(0);
            int line = 1;
            while (rset.next()) {
                String emailAdresse = rset.getString(7);
                if (emailAdresse == null) {
                    line = writeVerteilerRow(rset, sheet, line, betreff, text);
                } else {
                    bcc.append(emailAdresse).append(";");
                }
            }
            sendEmailWithAttachment(betreff, bcc.toString(), text, anhang);
            FileOutputStream fileOut = new FileOutputStream(reports + "/Verteiler.xls");
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Verteiler template not found", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write Verteiler report", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query Verteiler data", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        String path = new File(vorlagen + "\\Verteiler.dot").getAbsolutePath();
        try {
            Runtime.getRuntime().exec(excel + "\\winword.exe -z\"" + path + "\"");
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "Failed to launch Word for Verteiler", e1);
        }
    }

    private StringBuilder collectMultipleValues(String key) {
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

    private String getStringValue(String key, String field) {
        Object v = ((CProperties) p.get(key)).get(field);
        return v != null ? v.toString().trim() : "";
    }

    private void sendEmailWithAttachment(String betreff, String bcc, String text, String anhang) {
        try {
            Outlook outlookApplication = new Outlook();
            OutlookFolder outbox = outlookApplication.getDefaultFolder(FolderType.OUTBOX);
            OutlookMail mail = (OutlookMail) outbox.createItem(ItemType.MAIL);
            mail.setSubject(betreff);
            mail.setTo(CPropertyManager.getInstance().getProperty("email"));
            mail.setBCC(bcc);
            mail.setBody(text);
            File file = new File(anhang);
            if (file.isFile()) mail.getAttachments().addFile(file);
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
