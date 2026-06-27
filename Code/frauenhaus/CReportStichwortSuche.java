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

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CMessage;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;
import compucrash.CReportFrame;
import compucrash.CTable;

public class CReportStichwortSuche extends CCommand implements CReport {

	private CProperties p;
	private NumberFormat nf = NumberFormat.getInstance();
	private String reports;
	private String vorlagen;
	private String excel;
		
	public CReportStichwortSuche() {
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
		
		try {
			fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/StichwortSuche.xls"));
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
			SQLString += " ORDER BY m.name, m.vorname";
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
		        row = sheet.createRow(line++);
			    for (int i = 0; i < rsmd.getColumnCount(); i++) {
			        cell = row.createCell((short)(i));
			        cell.setCellValue(rset.getString(i + 1));
			    }			    
			}

			FileOutputStream fileOut = new FileOutputStream(reports + "\\StichwortSuche.xls");
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
            CMessage.print(excel + "\\excel.exe " + reports + "\\StichwortSuche.xls");
            p = r.exec(excel + "\\excel.exe " + reports + "\\StichwortSuche.xls");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
	}

	public void set(CProperties p) {
		this.p = p;
	}

}
