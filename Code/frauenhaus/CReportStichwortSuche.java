package frauenhaus;

import compucrash.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportStichwortSuche extends CCommand implements CReport {

    private static final String LABEL = "label";
    private static final Logger LOGGER = Logger.getLogger(CReportStichwortSuche.class.getName());
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportStichwortSuche() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    private static void writeAllRows(ResultSet rset, HSSFSheet sheet) throws SQLException {
        ResultSetMetaData rsmd = rset.getMetaData();
        int line = 0;
        HSSFRow headerRow = sheet.createRow(line++);
        for (int i = 0; i < rsmd.getColumnCount(); i++) {
            headerRow.createCell((short) i).setCellValue(rsmd.getColumnLabel(i + 1));
        }
        while (rset.next()) {
            HSSFRow row = sheet.createRow(line++);
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                row.createCell((short) i).setCellValue(rset.getString(i + 1));
            }
        }
    }

    public Object execute(Object parameters) {

        p = new CProperties();
        p.put("this", this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put(LABEL, "SpenderInnen mit einem oder mehreren Stichworten");
        pA.put("height", "150");
        pA.put("multiple", "1");
        pA.put("source", "stichwort");
        pA.put("columns", "1");
        pA = new CProperties();
        p.put(Integer.toString(3), pA);
        pA.put(LABEL, "F�rderverein");
        pA.put("check", "1");
        pA = new CProperties();
        p.put(Integer.toString(4), pA);
        pA.put(LABEL, "Frauenhaus");
        pA.put("check", "1");
        new CReportFrame(p);

        return null;
    }

    public void go() {
        StringBuilder verteiler = collectSelectedValues("1");
        String foerderverein = getCheckFilter("3", "AND m.foerderverein = 1 ");
        String frauenhaus = getCheckFilter("4", "AND m.frauenhaus = 1 ");

        try {
            FileOutputStream fileOut;
            try (POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/StichwortSuche.xls"))) {
                try (HSSFWorkbook wb = new HSSFWorkbook(fsin)) {
                    String sqlString = "SELECT DISTINCT m.* FROM frauenhaus.mitglied m " +
                            "WHERE m.mitglied IN (SELECT DISTINCT mitglied " +
                            "FROM frauenhaus.stichwort_person WHERE stichwort IN (" + verteiler + ")) " +
                            foerderverein + frauenhaus + " ORDER BY m.name, m.vorname";
                    ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(sqlString);
                    HSSFSheet sheet = wb.getSheetAt(0);
                    writeAllRows(rset, sheet);
                    fileOut = new FileOutputStream(reports + "\\StichwortSuche.xls");
                    wb.write(fileOut);
                }
            }
            fileOut.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Report template file not found", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write StichwortSuche report", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query StichwortSuche data", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            new ProcessBuilder(excel + "\\excel.exe " + reports + "\\StichwortSuche.xls").start();
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "Failed to open StichwortSuche in Excel", e1);
        }
    }

    private StringBuilder collectSelectedValues(String key) {
        return getStringBuilder(key, p);
    }

    static StringBuilder getStringBuilder(String key, CProperties p) {
        StringBuilder sb = new StringBuilder();
        if (((CProperties) p.get(key)).get("multipleValue") != null) {
            CTable tab = ((CTable) ((CProperties) p.get(key)).get("multipleValue"));
            getValueStringBuilder(sb, tab);
        }
        return sb;
    }

    static void getValueStringBuilder(StringBuilder sb, CTable tab) {
        int[] rows = tab.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            sb.append("'").append(tab.getValueAt(rows[i], 0).toString().trim()).append("',");
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
    }

    private boolean isChecked(String key) {
        Object v = ((CProperties) p.get(key)).get("checkValue");
        return v != null && (Boolean) v;
    }

    private String getCheckFilter(String key, String filter) {
        return isChecked(key) ? filter : "";
    }

    public void set(CProperties p) {
        this.p = p;
    }

}
