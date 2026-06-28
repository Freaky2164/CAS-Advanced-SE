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

public class CReportSerienbrief extends CCommand implements CReport {

    private static final String LABEL = "label";
    private static final String CHECK = "check";
    private static final Logger LOGGER = Logger.getLogger(CReportSerienbrief.class.getName());
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportSerienbrief() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    private static String buildSerienbriefSql(StringBuilder verteiler, String foerderverein,
                                              String frauenhaus, boolean eMail) {
        String sql = "SELECT DISTINCT m.* FROM frauenhaus.mitglied m " +
                "WHERE m.mitglied IN (SELECT DISTINCT mitglied " +
                "FROM frauenhaus.stichwort_person WHERE stichwort IN (" + verteiler + ")) ";
        sql += foerderverein + frauenhaus;
        sql += eMail ? " ORDER BY m.email, m.name, m.vorname" : " ORDER BY m.name, m.vorname";
        return sql;
    }

    private static int writeResultRows(ResultSet rset, ResultSetMetaData rsmd, HSSFSheet sheet,
                                       int line, boolean eMail, StringBuilder bcc) throws SQLException {
        while (rset.next()) {
            if (eMail && rset.getString("email") != null) {
                bcc.append(rset.getString("email")).append(";");
            } else {
                HSSFRow row = sheet.createRow(line++);
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    row.createCell((short) i).setCellValue(rset.getString(i + 1));
                }
            }
        }
        return line;
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
        pA.put(LABEL, "Förderverein");
        pA.put(CHECK, "1");
        pA = new CProperties();
        p.put(Integer.toString(4), pA);
        pA.put(LABEL, "Frauenhaus");
        pA.put(CHECK, "1");
        pA = new CProperties();
        p.put(Integer.toString(5), pA);
        pA.put(LABEL, "Briefkopf F�rderverein (sonst Frauenhaus)");
        pA.put(CHECK, "1");
        new CReportFrame(p);

        return null;
    }

    public void go() {
        StringBuilder verteiler = collectSelectedValues();
        String foerderverein = getCheckFilter("3", "AND m.foerderverein = 1 ");
        String frauenhaus = getCheckFilter("4", "AND m.frauenhaus = 1 ");
        String briefkopf = isChecked("5") ? "FVSerienBrief.dot" : "FHSerienBrief.dot";
        boolean eMail = false;
        StringBuilder bcc = new StringBuilder();

        try {
            HSSFWorkbook wb;
            try (POIFSFileSystem fsin = new POIFSFileSystem(new FileInputStream(vorlagen + "/Serienbrief.xls"))) {
                wb = new HSSFWorkbook(fsin);
            }
            String sqlString = buildSerienbriefSql(verteiler, foerderverein, frauenhaus, eMail);
            ResultSet rset = CDataManager.getInstance().getStatement().executeQuery(sqlString);
            HSSFSheet sheet = wb.getSheetAt(0);
            int line = 0;
            ResultSetMetaData rsmd = rset.getMetaData();
            HSSFRow row = sheet.createRow(line++);
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                row.createCell((short) i).setCellValue(rsmd.getColumnLabel(i + 1));
            }
            writeResultRows(rset, rsmd, sheet, line, eMail, bcc);
            FileOutputStream fileOut = new FileOutputStream(reports + "\\Serienbrief.xls");
            wb.write(fileOut);
            fileOut.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Serienbrief template not found", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write Serienbrief report", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query Serienbrief data", e);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            new ProcessBuilder(excel + "\\winword.exe /z\"" + vorlagen + "\\" + briefkopf + "\"").start();
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, "Failed to launch Word for Serienbrief", e1);
        }
    }

    private StringBuilder collectSelectedValues() {
        StringBuilder sb = new StringBuilder();
        if (((CProperties) p.get("1")).get("multipleValue") != null) {
            collectSelectedValuesHelper(sb, p);
        }
        return sb;
    }

    static void collectSelectedValuesHelper(StringBuilder sb, CProperties p) {
        CTable tab = ((CTable) ((CProperties) p.get("1")).get("multipleValue"));
        CReportStichwortSuche.getValueStringBuilder(sb, tab);
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
