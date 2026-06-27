package frauenhaus;

import java.awt.Toolkit;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.NumberFormat;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import ch.kova.connector.exception.ComponentObjectModelException;
import ch.kova.connector.exception.ItemNotFoundException;
import ch.kova.connector.exception.LibraryNotFoundException;
import ch.kova.connector.ms.outlook.Outlook;
import ch.kova.connector.ms.outlook.folder.FolderType;
import ch.kova.connector.ms.outlook.folder.OutlookFolder;
import ch.kova.connector.ms.outlook.item.ItemType;
import ch.kova.connector.ms.outlook.mail.OutlookMail;

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;
import compucrash.CReportFrame;
import compucrash.CTable;

public class CReportSerienbrief extends CCommand implements CReport {
    
    private CProperties p;
    private NumberFormat nf = NumberFormat.getInstance();
    private String reports;
    private String vorlagen;
    private String excel;
    
    public CReportSerienbrief() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }
    
    public Object execute(Object parameters) {
        
        p = new CProperties();
        p.put("this",this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1),pA);
        pA.put("label", "SpenderInnen mit einem oder mehreren Stichworten");
        pA.put("height", "150");
        pA.put("multiple", "1");
        pA.put("source","stichwort");
        pA.put("columns","1");
        /*		pA = new CProperties();
         p.put(Integer.toString(2),pA);
         pA.put("label", "...aber nicht mit Stichwort");
         pA.put("height", "150");
         pA.put("multiple", "1");
         pA.put("source","stichwort");*/
        pA = new CProperties();
        p.put(Integer.toString(3),pA);
        pA.put("label", "Förderverein");
        pA.put("check", "1");
        pA = new CProperties();
        p.put(Integer.toString(4),pA);
        pA.put("label", "Frauenhaus");
        pA.put("check", "1");
        pA = new CProperties();
        p.put(Integer.toString(5),pA);
        pA.put("label", "Briefkopf Förderverein (sonst Frauenhaus)");
        pA.put("check", "1");
//        pA = new CProperties();
//        p.put(Integer.toString(6),pA);
//        pA.put("label", "Email wenn möglich");
//        pA.put("check", "1");
        new CReportFrame(p);
        
        return null;
    }
    
    public void go() {
        // Datei öffnen
        POIFSFileSystem fsin;
        HSSFWorkbook wb;
        HSSFSheet sheet;
        HSSFRow row;
        HSSFCell cell;
        String verteiler = "";
        String ohne = "";
        String foerderverein = "";
        String frauenhaus = "";
        String briefkopf = "FHSerienBrief.dot";
        String bcc = "";
        String betreff = "";
        boolean eMail = false;
        
        if (((CProperties)p.get("1")).get("multipleValue") != null) {
            CTable tab = ((CTable)((CProperties)p.get("1")).get("multipleValue"));
            int[] rows = tab.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                verteiler += "'" + tab.getValueAt(rows[i],0).toString().trim() + "',";
            }
            verteiler = verteiler.substring(0, verteiler.length()-1);
        }
        /*		if (((CProperties)p.get("2")).get("multipleValue") != null) {
         CTable tab = ((CTable)((CProperties)p.get("2")).get("multipleValue"));
         int[] rows = tab.getSelectedRows();
         for (int i = 0; i < rows.length; i++) {
         ohne += "'" + tab.getValueAt(rows[i],0).toString().trim() + "',";
         }
         if (ohne.length() > 0) {
         ohne = ohne.substring(0, ohne.length()-1);			    
         }
         }*/
        if (((CProperties)p.get("3")).get("checkValue") != null) {
            if (((Boolean)((CProperties)p.get("3")).get("checkValue")).booleanValue()) {
                foerderverein = "AND m.foerderverein = 1 ";			    
            }
        }
        if (((CProperties)p.get("4")).get("checkValue") != null) {
            if (((Boolean)((CProperties)p.get("4")).get("checkValue")).booleanValue()) {
                frauenhaus = "AND m.frauenhaus = 1 ";			    
            }
        }
        if (((CProperties)p.get("5")).get("checkValue") != null) {
            if (((Boolean)((CProperties)p.get("5")).get("checkValue")).booleanValue()) {
                briefkopf = "FVSerienBrief.dot";
                
            }
        }
/*        if (((CProperties)p.get("6")).get("checkValue") != null) {
            if (((Boolean)((CProperties)p.get("6")).get("checkValue")).booleanValue()) {
                eMail = true;		    
            }
        }
*/        
        try {
            fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/Serienbrief.xls"));
            wb = new HSSFWorkbook(fsin);
            String SQLString = "SELECT DISTINCT m.* " +
            "FROM frauenhaus.mitglied m " +
            "WHERE m.mitglied IN (SELECT DISTINCT mitglied " +
            "FROM frauenhaus.stichwort_person WHERE stichwort IN (" + verteiler + ")) ";
            /*			if (ohne.length() > 0) {
             SQLString += "AND m.mitglied NOT IN (SELECT DISTINCT mitglied " +
             "FROM frauenhaus.stichwort_person WHERE stichwort IN (" + ohne + ")) ";
             } */
            SQLString += foerderverein;
            SQLString += frauenhaus;
            if (eMail) {
                SQLString += " ORDER BY m.email, m.name, m.vorname";
            } else {
                SQLString += " ORDER BY m.name, m.vorname";			    
            }
            
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
            sheet = wb.getSheetAt(0);
            int line = 0;
            ResultSetMetaData rsmd = rset.getMetaData();			
            row = sheet.createRow(line++);
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                cell = row.createCell((short)(i));
                cell.setCellValue(rsmd.getColumnLabel(i + 1));
            }			
            while (rset.next()) {
                if (eMail && rset.getString("email") != null) {
                    bcc += rset.getString("email") + ";";
                } else {
                    row = sheet.createRow(line++);
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        cell = row.createCell((short)(i));
                        cell.setCellValue(rset.getString(i + 1));
                    }			    
                }
            }
            FileOutputStream fileOut = new FileOutputStream(reports + "\\Serienbrief.xls");
            wb.write(fileOut);
            fileOut.close();
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec(excel + "\\winword.exe /z\"" + vorlagen + "\\" + briefkopf + "\"");
            System.out.println(excel + "\\winword.exe /z\"" + vorlagen + "\\" + briefkopf + "\"");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        // eMail
        if (eMail && bcc.trim().length() > 0) {
            try {
                // Outlook application
                Outlook outlookApplication = new Outlook();
                
                // Get the Outbox folder
                OutlookFolder outbox = outlookApplication.getDefaultFolder(FolderType.OUTBOX);
                
                // Create a new mail in the outbox folder
                OutlookMail mail = (OutlookMail) outbox.createItem(ItemType.MAIL);
                
                // Set the subject, destination and contents of the mail
                mail.setSubject(betreff);
                mail.setTo(CPropertyManager.getInstance().getProperty("email"));
                mail.setBCC(bcc);
                mail.setBody(" ");
                //		         File file = new File(anhang);
                //		         if (file.isFile()) mail.getAttachments().addFile(file);
                // Send the mail
                //		         mail.send();
                mail.display();
                // Dispose the library
                outlookApplication.dispose();
            }
            catch (ItemNotFoundException ex)
            {
                System.out.println("The outbox folder hasn't been found: ");
                ex.printStackTrace();
            }
            catch(ComponentObjectModelException ex)
            {
                System.out.println("COM error has occured: ");
                ex.printStackTrace();
            }
            catch(LibraryNotFoundException ex)
            {
                // If this error occurs, verify the file 'joutlookconnector.dll' is present
                // in java.library.path
                System.out.println("The Java Outlook Library has not been found.");
                ex.printStackTrace();
            }
        }
        // eMail ende

    }
    
    public void set(CProperties p) {
        this.p = p;
    }
    
}
