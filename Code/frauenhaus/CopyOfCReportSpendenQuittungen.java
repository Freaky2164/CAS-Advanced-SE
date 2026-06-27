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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

public class CopyOfCReportSpendenQuittungen extends CCommand implements CReport {
    
    private CProperties p;
    private NumberFormat nf = NumberFormat.getInstance();
    private String reports;
    private String vorlagen;
    private String excel;
    private POIFSFileSystem fsin;
    private HSSFWorkbook wb;
    private HSSFSheet sheet;
    private HSSFRow row;
    private HSSFCell cell;
    private String jahr = "";
    private DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public CopyOfCReportSpendenQuittungen() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }
    
    public Object execute(Object parameters) {
        
        p = new CProperties();
        p.put("this",this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1),pA);
        pA.put("label", "Jahr");
        pA.put("equals", "1");
        new CReportFrame(p);
        
        return null;
    }
    
    public void go() {
        // Jahr setzen und Reports aufrufen        
        if (((CProperties)p.get("1")).get("equalsValue") != null) {
            if (!((CProperties) p.get("1")).get("equalsValue").toString().trim().equalsIgnoreCase("")) {
                String value = ((CProperties) p.get("1")).get("equalsValue").toString().trim();
                jahr = value;
            }
        }
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        report("Frauenhaus","Dauerspende");
        report("Frauenhaus","Mitgliedsbeitrag");
        report("Förderverein","Dauerspende");
//        report("Förderverein","Mitgliedsbeitrag");
    }
    
    private void report(String verein, String spendenart) {
        System.out.println("Report: " + verein + " " + spendenart);
        try {
            fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/SpendenQuittungen.xls"));
            wb = new HSSFWorkbook(fsin);
            String SQLString = "SELECT DISTINCT m.mitglied, m.anrede, m.vorname, m.name, m.name2, m.name3, m.strasse, m.plz, m.ort " +
            	"FROM frauenhaus.mitglied m " +
            	"WHERE EXISTS (SELECT * " +
            		"FROM frauenhaus.spende s " +
            		"WHERE s.mitglied = m.mitglied " +
            		"AND YEAR(s.datum) = " + jahr + " " +
            		"AND s.spendenart = '" + spendenart + "' " +
            		"AND s.verein = '" + verein + "')";
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
            sheet = wb.getSheetAt(0);
            int line = 1;
            while (rset.next()) {
                row = sheet.createRow(line++);
                String mitglied = rset.getString(1);
                cell = row.createCell((short)0);
                cell.setCellValue(rset.getString(2));
                cell = row.createCell((short)1);
                cell.setCellValue(rset.getString(3));
                cell = row.createCell((short)2);
                cell.setCellValue(rset.getString(4));
                cell = row.createCell((short)3);
                cell.setCellValue(rset.getString(5));
                cell = row.createCell((short)4);
                cell.setCellValue(rset.getString(6));
                cell = row.createCell((short)5);
                cell.setCellValue(rset.getString(7));
                cell = row.createCell((short)6);
                cell.setCellValue(rset.getString(8));
                cell = row.createCell((short)7);
                cell.setCellValue(rset.getString(9));
                cell = row.createCell((short)8);
                cell.setCellValue(verein);
                cell = row.createCell((short)9);
                cell.setCellValue(spendenart);
                cell = row.createCell((short)10);
                cell.setCellValue(jahr);
                // Gesamtbetrag, BetragInWorten, Beträge
                SQLString = "SELECT datum, betrag " +
                	"FROM frauenhaus.spende " +
                	"WHERE mitglied = " + mitglied + " " +
                	"AND YEAR(datum) = " + jahr + " " +
                	"AND spendenart = '" + spendenart + "' " +
                	"AND verein = '" + verein + "' " +
                	"ORDER BY datum";
                ResultSet rset0 = CDataManager.getInstance().getStatement().executeQuery(SQLString);
                double betrag = 0;
                String betraege = "";
                while(rset0.next()) {
                    LocalDate datum = rset0.getDate(1).toLocalDate();
                    double einzelbetrag = rset0.getDouble(2);
                    betraege += fmt.format(datum) + "\t" + nf.format(einzelbetrag) + "\n";
                    betrag += einzelbetrag;                    
                }
                if (betraege.length() > 0) betraege = betraege.substring(0, betraege.length() - 1);
                cell = row.createCell((short)11);
                cell.setCellValue(nf.format(betrag));
                cell = row.createCell((short)12);
                cell.setCellValue(betragInWorten(nf.format(betrag)));
                cell = row.createCell((short)13);
                cell.setCellValue(betraege);
            }
            FileOutputStream fileOut = new FileOutputStream(reports + "\\SpendenQuittung" + verein + spendenart + ".xls");
            wb.write(fileOut);
            fileOut.close();
            Runtime.getRuntime().exec(excel + "\\winword.exe /z\"" + vorlagen + "\\SpendenQuittung" + verein + spendenart + ".dot\"");
            System.out.println(excel + "\\winword.exe /z\"" + vorlagen + "\\SpendenQuittung" + verein + spendenart + ".dot\"");
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
        } catch (IOException e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
        } catch (SQLException e) {
            e.printStackTrace();
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private String betragInWorten(String zahl) {
        char[] ziffer = zahl.toCharArray();
        String wert = "";
        for (int i = 0; i < ziffer.length; i++) {
            switch (ziffer[i]) {
            case '1':
                wert += "Eins - ";
                break;
            case '2':
                wert += "Zwei - ";
                break;
            case '3':
                wert += "Drei - ";
                break;
            case '4':
                wert += "Vier - ";
                break;
            case '5':
                wert += "Fünf - ";
                break;
            case '6':
                wert += "Sechs - ";
                break;
            case '7':
                wert += "Sieben - ";
                break;
            case '8':
                wert += "Acht - ";
                break;
            case '9':
                wert += "Neun - ";
                break;
            case '0':
                wert += "Null - ";
                break;
            case ',':
                wert += "Komma - ";
                break;
            }
        }
        wert = wert.substring(0,wert.length() - 2);
        return wert;
    }
    
    public void set(CProperties p) {
        this.p = p;
    }
    
}
