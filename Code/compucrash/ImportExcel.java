
package compucrash;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ImportExcel {
	
	private int docno = 0;
	String outputLine;
	File fout;
	FileOutputStream out;
	private Connection conn;
	private Statement stmt;

	public ImportExcel() {
		// open file
		POIFSFileSystem fs;
		String s = "";
		String rowData[] = new String[21];
		try {
			Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
			conn = DriverManager.getConnection("jdbc:odbc:msserver", "anwender", "anwender");
			stmt = conn.createStatement();
		} catch (ClassNotFoundException e) {
			System.out.println(e);
			System.exit(0);
		} catch (SQLException se) {
			System.out.println(se);
			System.exit(0);
		}
		
		try {
			fout = new File("worksheet.sql");
			out = new FileOutputStream(fout);
			fs = new POIFSFileSystem(new FileInputStream("worksheet.xls"));
			HSSFWorkbook wb = new HSSFWorkbook(fs);
			HSSFSheet sheet = wb.getSheetAt(0);
			for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
				HSSFRow row = sheet.getRow((short) i);
				for (int j = 0; j < 21; j++) {
					HSSFCell cell = row.getCell((short) j);
					if (cell == null) cell = row.createCell((short) j);
					switch (cell.getCellType()) {
						case HSSFCell.CELL_TYPE_STRING:
							s = cell.getStringCellValue();
							break;
						case HSSFCell.CELL_TYPE_NUMERIC:
							s = Double.toString(cell.getNumericCellValue());
							break;
						default:
							s = cell.getStringCellValue();
					}	
					rowData[j] = s;
				}
				insert(rowData);
			}
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println(e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	private void insert(String[] rowData) throws IOException, SQLException {
		ResultSet rset = stmt.executeQuery("SELECT max(docid) FROM biopharm.mp52");
		docno = 1;
		if (rset.next()) docno = rset.getInt(1) + 1;		
		String docNoString = Integer.toString(docno);
		outputLine = "";
		String SQLString = "INSERT INTO biopharm.mp52 VALUES (" + docNoString + ",";
		for (int i = 0; i < rowData.length; i++) {
			SQLString += "'" + rowData[i].replaceAll("'","''") + "',";
		}
		SQLString = SQLString.substring(0,SQLString.length() -1);
		SQLString += ");";
		System.out.println(SQLString);
		stmt.execute(SQLString);
	}

	public static void main(String[] args) {
		new ImportExcel();
	}
}