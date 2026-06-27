package compucrash;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImportExcel {

    private static final Logger LOGGER = Logger.getLogger(ImportExcel.class.getName());

    String outputLine;
    File fout;
    FileOutputStream out;
    private final int docno = 0;
    private Connection conn;
    private Statement stmt;

    public ImportExcel() {
        // open file
        POIFSFileSystem fs;
        String s = "";
        String[] rowData = new String[21];
        try {
            Properties p = CPropertyManager.getInstance().getProperties();
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://"
                            + p.getProperty("dbhost")
                            + ":"
                            + p.getProperty("dbport")
                            + "/"
                            + p.getProperty("dbsid"),
                    p.getProperty("dbuser"),
                    p.getProperty("dbpwd"));
            stmt = conn.createStatement();
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "JDBC driver not found", e);
            System.exit(0);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database connection failed", se);
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
            LOGGER.log(Level.SEVERE, "Worksheet file not found", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read worksheet", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert worksheet data", e);
        }
    }

    static void main(String[] args) {
        new ImportExcel();
    }

    private void insert(String[] rowData) throws IOException, SQLException {
        int docNo = 1;
        try (ResultSet rset = stmt.executeQuery("SELECT max(docid) FROM biopharm.mp52")) {
            if (rset.next()) docNo = rset.getInt(1) + 1;
        }
        outputLine = "";
        StringBuilder sqlString = new StringBuilder("INSERT INTO biopharm.mp52 VALUES (?");
        for (int i = 0; i < rowData.length; i++) {
            sqlString.append(",?");
        }
        sqlString.append(")");
        try (PreparedStatement pstmt = conn.prepareStatement(sqlString.toString())) {
            pstmt.setInt(1, docNo);
            for (int i = 0; i < rowData.length; i++) {
                pstmt.setString(i + 2, rowData[i]);
            }
            pstmt.executeUpdate();
        }
    }
}
