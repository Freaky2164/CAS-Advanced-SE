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
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
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

public class CReportSpendenUebersicht extends CCommand implements CReport {
    
    private CProperties p;
    private NumberFormat nf = NumberFormat.getInstance();
    private String reports;
    private String vorlagen;
    private String excel;
    
    public CReportSpendenUebersicht() {
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
        // Datei öffnen
        POIFSFileSystem fsin;
        HSSFWorkbook wb;
        HSSFSheet sheet;
        HSSFRow row;
        HSSFCell cell;
        String jahr = "";
        HSSFCellStyle gelbTitelStyle;
        HSSFCellStyle gelbUntertitelStyle;
        HSSFCellStyle gelbDatumStyle;
        HSSFCellStyle gelbGeldStyle;
        HSSFCellStyle nameStyle;
        HSSFCellStyle vornameStyle;
        HSSFCellStyle datumStyle;
        HSSFCellStyle geldStyle;
        
		if (((CProperties) p.get("1")).get("equalsValue") != null) {
			if (!((CProperties) p.get("1")).get("equalsValue").toString().trim().equalsIgnoreCase("")) {
				jahr = " AND year(s.datum) = '" + ((CProperties) p.get("1")).get("equalsValue").toString().trim() + "' ";
			}
		}
        
        try {
            fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/SpendenUebersicht.xls"));
            wb = new HSSFWorkbook(fsin);
            String SQLString = "SELECT s.verein, a.spendentyp, s.spendenart, m.name, m.vorname, s.datum, s.betrag " +
            	"FROM frauenhaus.mitglied m, frauenhaus.spende s, frauenhaus.spendenart a " +
            	"WHERE s.mitglied = m.mitglied " +
            	"AND s.spendenart = a.spendenart " +
            	jahr + 
            	" ORDER BY s.verein, a.spendentyp, s.spendenart, m.name, m.vorname, s.datum ";			                
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
            sheet = wb.getSheetAt(0);
            gelbTitelStyle = sheet.getRow(0).getCell((short)0).getCellStyle();
            gelbUntertitelStyle = sheet.getRow(0).getCell((short)1).getCellStyle();
            gelbDatumStyle = sheet.getRow(0).getCell((short)2).getCellStyle();
            gelbGeldStyle = sheet.getRow(0).getCell((short)3).getCellStyle();
            nameStyle = sheet.getRow(1).getCell((short)0).getCellStyle();
            vornameStyle= sheet.getRow(1).getCell((short)1).getCellStyle();
            datumStyle = sheet.getRow(1).getCell((short)2).getCellStyle();
            geldStyle = sheet.getRow(1).getCell((short)3).getCellStyle();
            int line = -1;
            ResultSetMetaData rsmd = rset.getMetaData();
            String verein = "init";
            String spendentyp = "init";
            String spendenart = "init";
            double sum = 0;
            double summeGesamt = 0;
            boolean sumLine = false;
            while (rset.next()) {
                //Zeile aufbauen
                //Kopfzeilen
                //Verein
                if (!rset.getString(1).equalsIgnoreCase(verein)) {
                    if (!spendentyp.equalsIgnoreCase("init")) {
                        row = sheet.createRow(line++);
                        cell = row.createCell((short) 0);
                        cell.setCellValue(spendentyp + " Gesamt:");
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 1);
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 2);
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 3);
                        cell.setCellStyle(gelbGeldStyle);
                        cell.setCellValue(sum);
                        sum = 0;
                        sumLine = true;
                        //Summe gesamt
                        row = sheet.createRow(line++);
                        cell = row.createCell((short) 0);
                        cell.setCellValue("Gesamt:");
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 1);
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 2);
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 3);
                        cell.setCellStyle(gelbGeldStyle);
                        cell.setCellValue(summeGesamt);
                        summeGesamt = 0;
                        
                    }
                    verein = rset.getString(1);
                    row = sheet.createRow(line++);
                    row = sheet.createRow(line++);
                    cell = row.createCell((short) 0);
                    cell.setCellValue(verein);
                    cell.setCellStyle(gelbTitelStyle);
                    cell = row.createCell((short) 1);
                    cell.setCellStyle(gelbTitelStyle);
                    cell = row.createCell((short) 2);
                    cell.setCellStyle(gelbTitelStyle);
                    cell = row.createCell((short) 3);
                    cell.setCellStyle(gelbTitelStyle);
                }
                //Spendentyp
                if (!rset.getString(2).equalsIgnoreCase(spendentyp)) {
                    //Summe
                    if (!sumLine && !spendentyp.equalsIgnoreCase("init")) {
                        row = sheet.createRow(line++);
                        cell = row.createCell((short) 0);
                        cell.setCellValue(spendentyp + " Gesamt:");
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 1);
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 2);
                        cell.setCellStyle(gelbTitelStyle);
                        cell = row.createCell((short) 3);
                        cell.setCellStyle(gelbGeldStyle);
                        cell.setCellValue(sum);
                        sum = 0;
                    }
                    sumLine = false;
                    
                    row = sheet.createRow(line++);
                    spendentyp = rset.getString(2);
                    row = sheet.createRow(line++);
                    cell = row.createCell((short) 0);
                    cell.setCellValue(spendentyp);
                    cell.setCellStyle(gelbTitelStyle);
                    cell = row.createCell((short) 1);
                    cell.setCellStyle(gelbTitelStyle);
                }
                //Spendenart
                if (!rset.getString(3).equalsIgnoreCase(spendenart)) {
                    spendenart = rset.getString(3);
                    row = sheet.createRow(line++);
                    cell = row.createCell((short) 0);
                    cell.setCellValue(spendenart);
                    cell.setCellStyle(gelbTitelStyle);
                }
                //Datenzeile
                row = sheet.createRow(line++);
                cell = row.createCell((short) 0);
                cell.setCellValue(rset.getString(4));
                cell = row.createCell((short) 1);
                cell.setCellValue(rset.getString(5));
                cell = row.createCell((short) 2);
                cell.setCellValue(rset.getDate(6));
                cell.setCellStyle(datumStyle);
                cell = row.createCell((short) 3);
                cell.setCellValue(rset.getDouble(7));
                cell.setCellStyle(geldStyle);
                sum += rset.getDouble(7);
                summeGesamt += rset.getDouble(7);
            }
            //Zusammenfassung am Ende
            row = sheet.createRow(line++);
            cell = row.createCell((short) 0);
            cell.setCellValue(spendentyp + " Gesamt:");
            cell.setCellStyle(gelbTitelStyle);
            cell = row.createCell((short) 1);
            cell.setCellStyle(gelbTitelStyle);
            cell = row.createCell((short) 2);
            cell.setCellStyle(gelbTitelStyle);
            cell = row.createCell((short) 3);
            cell.setCellStyle(gelbGeldStyle);
            cell.setCellValue(sum);
            sum = 0;
            //Summe gesamt
            row = sheet.createRow(line++);
            cell = row.createCell((short) 0);
            cell.setCellValue("Gesamt:");
            cell.setCellStyle(gelbTitelStyle);
            cell = row.createCell((short) 1);
            cell.setCellStyle(gelbTitelStyle);
            cell = row.createCell((short) 2);
            cell.setCellStyle(gelbTitelStyle);
            cell = row.createCell((short) 3);
            cell.setCellStyle(gelbGeldStyle);
            cell.setCellValue(summeGesamt);
            summeGesamt = 0;

            FileOutputStream fileOut = new FileOutputStream(reports + "\\SpendenUebersicht.xls");
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
            p = r.exec(excel + "\\excel.exe " + reports + "\\SpendenUebersicht.xls");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    public void set(CProperties p) {
        this.p = p;
    }
    
}
