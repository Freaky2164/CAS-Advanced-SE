package frauenhaus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

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

public class CReportBussgeldUebersicht extends CCommand implements CReport {

	private CProperties p;
	private String reports;
	private String vorlagen;
	private String excel;
	private String dateFrom = "";
	private String dateTo ="";
	private POIFSFileSystem fsin;
	private HSSFWorkbook wb;
	private HSSFSheet sheet;
	private HSSFCellStyle style1;
	private HSSFCellStyle style2;
	private HSSFCellStyle style3;
	private short rowHeight;
	private int line;
	private HSSFRow row;
	private HSSFCell cell;
	
	
	public CReportBussgeldUebersicht() {
		this.reports = CPropertyManager.getInstance().getProperty("reports");
		this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
		this.excel = CPropertyManager.getInstance().getProperty("excel");
	}

	public Object execute(Object parameters) {
		
		p = new CProperties();
		p.put("this",this);
		p.put("title","Bußgelder Übersicht");
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
			fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/BußgeldÜbersicht.xls"));
			wb = new HSSFWorkbook(fsin);
			sheet = wb.getSheetAt(0);
			style1 = wb.getSheetAt(0).getRow((short)0).getCell((short)0).getCellStyle();
			style2 = wb.getSheetAt(0).getRow((short)0).getCell((short)1).getCellStyle();
			style3 = wb.getSheetAt(0).getRow((short)0).getCell((short)2).getCellStyle();
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

			FileOutputStream fileOut = new FileOutputStream(reports + "/BußgeldÜbersicht.xls");
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
			p = r.exec(excel + "\\excel.exe " + reports + "\\BußgeldÜbersicht.xls");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
private void compute (String verein) throws SQLException {
	String SQLString = "SELECT g.bezeichnung, (SELECT coalesce(sum(b.betrag),0) " +
	"FROM frauenhaus.bussgeld b " +
	"WHERE b.gericht = g.gericht " +
	"AND b.datum >= '" + dateFrom + "' " +
	"AND b.datum <= '" + dateTo + "' " +
	"AND b.verein = '" + verein + "') bussgeldbetrag, " +
	"(SELECT ISNULL(sum(e.betrag),0) " +
	"FROM frauenhaus.eingang e, frauenhaus.bussgeld b2 " +
	"WHERE b2.bussgeld = e.bussgeld " +
	"AND b2.gericht = g.gericht " +
	"AND e.datum >= '" + dateFrom + "' " +
	"AND e.datum <= '" + dateTo + "' " +
	"AND b2.verein = '" + verein + "') einzahlbetrag " +
	"FROM frauenhaus.gericht g " +
	"ORDER BY g.bezeichnung ";
//	System.out.println(SQLString);
	ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(SQLString);
		
	row = sheet.createRow(line++);
	row.setHeight(rowHeight);
	cell = row.createCell((short) 0);
	cell.setCellValue(verein);
	cell.setCellStyle(style1);
	cell = row.createCell((short) 1);
	cell.setCellValue("Bußgelder");
	cell.setCellStyle(style1);
	cell = row.createCell((short) 2);
	cell.setCellValue("Eingänge");
	cell.setCellStyle(style1);
	
	double bussgeld = 0;
	double eingang = 0;
	while (rset.next()) {
		row = sheet.createRow(line++);
		row.setHeight(rowHeight);
		cell = row.createCell((short) 0);
		cell.setCellValue(rset.getString(1));
		cell.setCellStyle(style2);
		cell = row.createCell((short) 1);
		cell.setCellValue(rset.getDouble(2));
		bussgeld += rset.getDouble(2);
		cell.setCellStyle(style2);
		cell = row.createCell((short) 2);
		cell.setCellValue(rset.getDouble(3));
		eingang += rset.getDouble(3);
		cell.setCellStyle(style2);
	}
	row = sheet.createRow(line++);
	row.setHeight(rowHeight);
	cell = row.createCell((short) 0);
	cell.setCellValue("Summe " + verein);
	cell.setCellStyle(style1);
	cell = row.createCell((short) 1);
	cell.setCellValue(bussgeld);
	cell.setCellStyle(style3);
	cell = row.createCell((short) 2);
	cell.setCellValue(eingang);
	cell.setCellStyle(style3);	
}

	public void set(CProperties p) {
		this.p = p;
	}

}
