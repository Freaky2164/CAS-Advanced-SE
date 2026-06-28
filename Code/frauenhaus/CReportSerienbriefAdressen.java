package frauenhaus;

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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static frauenhaus.CReportStichwortSuche.getStringBuilder;

public class CReportSerienbriefAdressen extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CReportSerienbriefAdressen.class.getName());
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportSerienbriefAdressen() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    private static int writeAddressRow(ResultSet rset, HSSFSheet sheet, int line) throws SQLException {
        HSSFRow row = sheet.createRow(line++);
        row.createCell((short) 0).setCellValue(rset.getString(1));
        row.createCell((short) 1).setCellValue(rset.getString(2));
        row.createCell((short) 2).setCellValue(rset.getString(3));
        row.createCell((short) 3).setCellValue(rset.getString(4));
        row.createCell((short) 4).setCellValue(rset.getString(5));
        row.createCell((short) 5).setCellValue(rset.getString(6));
        return line;
    }

    public Object execute(Object parameters) {

        p = new CProperties();
        p.put("this", this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "Adressen");
        pA.put("height", "150");
        pA.put("multiple", "1");
        pA.put("source", "mitglied");
        pA = new CProperties();
        p.put(Integer.toString(2), pA);
        pA.put("label", "Email-Text");
        pA.put("text", "1");
        new CReportFrame(p);

        return null;
    }

    public void go() {
        StringBuilder verteiler = collectMultipleValues("1");
        String text = getTextValue("2");
        String betreff = "";
        String anhang = "";
        StringBuilder bcc = new StringBuilder();

        try {
            HSSFWorkbook wb;
            try (POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/Verteiler.xls"))) {
                wb = new HSSFWorkbook(fsin);
            }
            String sqlString = "SELECT DISTINCT k.anrede, k.vorname, k.name, k.strasse, k.plz, k.ort, k.email, ' ' " +
                    "FROM frauenhaus.mitglied k WHERE k.mitglied IN (" + verteiler + ") ORDER BY k.email";
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(sqlString);
            HSSFSheet sheet = wb.getSheetAt(0);
            int line = 1;
            while (rset.next()) {
                String emailAdresse = rset.getString(7);
                if (emailAdresse == null) {
                    line = writeAddressRow(rset, sheet, line);
                } else {
                    bcc.append(emailAdresse).append(";");
                }
            }
            sendEmailWithAttachment(betreff, bcc.toString(), text, anhang);
            FileOutputStream fileOut = new FileOutputStream(reports + "/Verteiler.xls");
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Serienbrief template not found", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write SerienbriefAdressen report", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query SerienbriefAdressen data", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(new StringSelection(text), null);
        String path = new File(vorlagen + "/Verteiler.dot").getAbsolutePath();
        try {
            new ProcessBuilder(excel + "/winword.exe -t\"" + path + "\"").start();
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "Failed to launch Word for Serienbrief", e1);
        }
    }

    private StringBuilder collectMultipleValues(String key) {
        return getStringBuilder(key, p);
    }

    private String getTextValue(String key) {
        Object v = ((CProperties) p.get(key)).get("textValue");
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
