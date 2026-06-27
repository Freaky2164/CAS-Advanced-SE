package frauenhaus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;
import compucrash.CReportFrame;

public class CReportBussgeldDetail extends CCommand implements CReport {

	private CProperties p;
	private String reports;
	private String vorlagen;
	private String excel;
	private String dateFrom = "";
	private String dateTo ="";
	private POIFSFileSystem fsin;
	private HSSFWorkbook wb;
	private HSSFSheet sheet;
	private HSSFCellStyle styleDate;
	private HSSFCellStyle styleText;
	private HSSFCellStyle styleMoney;
	private short rowHeight;
	private int line;
	private HSSFRow row;
	private HSSFCell cell;
    private HSSFCellStyle styleTextTitle;
    private HSSFCellStyle styleDateTitle;
    private HSSFCellStyle styleMoneyTitle;
	
	
	public CReportBussgeldDetail() {
		this.reports = CPropertyManager.getInstance().getProperty("reports");
		this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
		this.excel = CPropertyManager.getInstance().getProperty("excel");
	}

	public Object execute(Object parameters) {
		
		p = new CProperties();
		p.put("this",this);
		p.put("title","Bußgelder Detail");
		CProperties pA = new CProperties();
		p.put(Integer.toString(1),pA);
		pA.put("label", "Zeitraum");
		pA.put("between", "1");
		new CReportFrame(p);

		return null;
	}

	public void go() {
		// Datei öffnen

		if (((CProperties) p.get("1")).get("fromValue") != null) {
			if (!((CProperties) p.get("1")).get("fromValue").toString().trim()
					.equalsIgnoreCase("")) {
				dateFrom = ((CProperties) p.get("1")).get("fromValue").toString().trim();
			}
		}
		if (((CProperties) p.get("1")).get("toValue") != null) {
			if (!((CProperties) p.get("1")).get("toValue").toString().trim()
					.equalsIgnoreCase("")) {
				dateTo = ((CProperties) p.get("1")).get("toValue").toString().trim();
			}
		}

		try {
			fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/BußgeldDetail.xls"));
			wb = new HSSFWorkbook(fsin);
			sheet = wb.getSheetAt(0);
			styleTextTitle = wb.getSheetAt(0).getRow((short)0).getCell((short)0).getCellStyle();
			styleDateTitle = wb.getSheetAt(0).getRow((short)0).getCell((short)1).getCellStyle();
			styleMoneyTitle = wb.getSheetAt(0).getRow((short)0).getCell((short)2).getCellStyle();
			styleText = wb.getSheetAt(0).getRow((short)1).getCell((short)0).getCellStyle();
			styleDate = wb.getSheetAt(0).getRow((short)1).getCell((short)1).getCellStyle();
			styleMoney = wb.getSheetAt(0).getRow((short)1).getCell((short)2).getCellStyle();
			rowHeight = wb.getSheetAt(0).getRow((short)0).getHeight();
			line = 0;
			compute("Förderverein");
			row = sheet.createRow(line++);
			row.setHeight(rowHeight);
			row = sheet.createRow(line++);
			row.setHeight(rowHeight);
			compute("Frauenhaus");
			
			// Header generieren
			String headerString = sheet.getHeader().getCenter();
			headerString = headerString.replaceFirst("ANFANG", dateFrom);
			headerString = headerString.replaceFirst("ENDE", dateTo);
			sheet.getHeader().setCenter(headerString);

			FileOutputStream fileOut = new FileOutputStream(reports + "/BußgeldDetail.xls");
		    wb.write(fileOut);
		    fileOut.close();
		    
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		Runtime r = Runtime.getRuntime();
		Process p = null;
		try {
			p = r.exec(excel + "\\excel.exe " + reports + "\\BußgeldDetail.xls");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
private void compute (String verein) throws SQLException {
    String SQLString = "SELECT b.datum, g.bezeichnung, coalesce(b.aktenzeichen, 'unbekannt'), " +
    		"b.name + ', ' + b.vorname, b.betrag, " +
    		"(SELECT SUM(betrag) FROM frauenhaus.eingang WHERE bussgeld = b.bussgeld) offen, " +
    		"e.datum, e.betrag " +
    		"FROM frauenhaus.gericht g, frauenhaus.bussgeld b, frauenhaus.eingang e " +
    		"WHERE g.gericht = b.gericht " +
    		"AND b.bussgeld = e.bussgeld " +
    		"AND e.datum >= '" + dateFrom + "' " +
    		"AND e.datum <= '" + dateTo + "' " +
    		"AND b.verein = '" + verein + "' " +
    		"ORDER BY b.datum, b.aktenzeichen, e.datum";
        
	System.out.println(SQLString);
	ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
		
	row = sheet.createRow(line++);
	row.setHeight(rowHeight);
	cell = row.createCell((short) 0);
	cell.setCellValue(" ");
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 1);
	cell.setCellValue(verein);
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 2);
	cell.setCellValue(" ");
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 3);
	cell.setCellValue(" ");
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 4);
	cell.setCellValue(" ");
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 5);
	cell.setCellValue(" ");
	cell.setCellStyle(styleTextTitle);
	row = sheet.createRow(line++);
	
	double bussgeld = 0;
	double eingang = 0;
	double summe = 0;
	Date datum;
	String gericht;
	String aktenzeichen;
	String name;
	String aktenzeichenAlt = "ANFANG";
	double betrag = 0;
	double offen = 0;
	while (rset.next()) {
	    datum = rset.getDate(1);
	    gericht = rset.getString(2);
	    aktenzeichen = rset.getString(3);
	    name = rset.getString(4);
	    betrag = rset.getDouble(5);
	    offen = rset.getDouble(6);
	    if (!aktenzeichenAlt.equalsIgnoreCase(aktenzeichen)) {
	        if (!aktenzeichenAlt.equalsIgnoreCase("ANFANG")) {
	            // Zusammenfassung
				row = sheet.createRow(line++);
				cell = row.createCell((short) 3);
				cell.setCellValue("Gesamt");
				cell.setCellStyle(styleTextTitle);
				cell = row.createCell((short) 4);
				cell.setCellValue(" ");
				cell.setCellStyle(styleTextTitle);
				cell = row.createCell((short) 5);
				cell.setCellValue(summe);
				cell.setCellStyle(styleMoneyTitle);
				summe = 0;
				row = sheet.createRow(line++);				
	        }
	        // Neues Bußgeld
			row = sheet.createRow(line++);
			row.setHeight(rowHeight);
			cell = row.createCell((short) 0);
			cell.setCellValue(datum);
			cell.setCellStyle(styleDateTitle);
			cell = row.createCell((short) 1);
			cell.setCellValue(gericht);
			cell.setCellStyle(styleTextTitle);
			cell = row.createCell((short) 2);
			cell.setCellValue(aktenzeichen);
			cell.setCellStyle(styleTextTitle);
			cell = row.createCell((short) 3);
			cell.setCellValue(name);
			cell.setCellStyle(styleTextTitle);
			cell = row.createCell((short) 4);
			cell.setCellValue("Betrag:");
			cell.setCellStyle(styleTextTitle);
			cell = row.createCell((short) 5);
			cell.setCellValue("Offen:");
			cell.setCellStyle(styleTextTitle);

			row = sheet.createRow(line++);
			cell = row.createCell((short) 4);
			cell.setCellValue(betrag);
			cell.setCellStyle(styleMoneyTitle);
			cell = row.createCell((short) 5);
			cell.setCellValue(betrag - offen);
			cell.setCellStyle(styleMoneyTitle);

			row = sheet.createRow(line++);
			cell = row.createCell((short) 3);
			cell.setCellValue("Eingänge");
			cell.setCellStyle(styleTextTitle);
			cell = row.createCell((short) 4);
			cell.setCellValue("Datum");
			cell.setCellStyle(styleTextTitle);
			cell = row.createCell((short) 5);
			cell.setCellValue("Betrag");
			cell.setCellStyle(styleTextTitle);

			aktenzeichenAlt = aktenzeichen;
	    }
	    // Eingänge
		row = sheet.createRow(line++);
		row.setHeight(rowHeight);
		cell = row.createCell((short) 4);
		cell.setCellValue(rset.getDate(7)); // Datum
		cell.setCellStyle(styleDate);
		cell = row.createCell((short) 5);
		cell.setCellValue(rset.getDouble(8)); // Betrag
		summe += rset.getDouble(8);
		cell.setCellStyle(styleMoney);
	}
	// Letzte Zusammenfassung
	row = sheet.createRow(line++);
	cell = row.createCell((short) 3);
	cell.setCellValue("Gesamt");
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 4);
	cell.setCellValue(" ");
	cell.setCellStyle(styleTextTitle);
	cell = row.createCell((short) 5);
	cell.setCellValue(summe);
	cell.setCellStyle(styleMoneyTitle);
}

	public void set(CProperties p) {
		this.p = p;
	}

}
