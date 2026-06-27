package frauenhaus;

import compucrash.*;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportStichworteZusammenfassen extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CReportStichworteZusammenfassen.class.getName());
    private final NumberFormat nf = NumberFormat.getInstance();
    private final String reports;
    private final String vorlagen;
    private final String excel;
    private CProperties p;

    public CReportStichworteZusammenfassen() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }

    public Object execute(Object parameters) {

        p = new CProperties();
        p.put("this", this);
        CProperties pA = new CProperties();
        p.put(Integer.toString(1), pA);
        pA.put("label", "Stichworte");
        pA.put("height", "150");
        pA.put("multiple", "1");
        pA.put("source", "stichwort");
        pA = new CProperties();
        p.put(Integer.toString(2), pA);
        pA.put("label", "Zusammenfassen in Stichwort");
        pA.put("equals", "1");
        pA.put("source", "stichwort");
        new CReportFrame(p);

        return null;
    }

    public void go() {
        StringBuilder stichworteAlt = new StringBuilder();
        String stichwortNeu = "";

        if (((CProperties) p.get("1")).get("multipleValue") != null) {
            CTable tab = ((CTable) ((CProperties) p.get("1")).get("multipleValue"));
            int[] rows = tab.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                stichworteAlt.append("'").append(tab.getValueAt(rows[i], 0).toString().trim()).append("',");
            }
            if (!stichworteAlt.isEmpty()) stichworteAlt.setLength(stichworteAlt.length() - 1);
        } else {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (((CProperties) p.get("2")).get("equalsValue") != null) {
            stichwortNeu = ((CProperties) p.get("2")).get("equalsValue").toString().trim();
        } else {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        try {
            // Neues Stichwort zuordnen
            String SQLString = "INSERT INTO frauenhaus.stichwort_person " +
                    "SELECT DISTINCT mitglied, '" + stichwortNeu + "' " +
                    "FROM frauenhaus.mitglied m " +
                    "WHERE m.mitglied IN (SELECT mitglied " +
                    "FROM frauenhaus.stichwort_person " +
                    "WHERE stichwort IN (" + stichworteAlt + ")) " +
                    "AND m.mitglied NOT IN (SELECT mitglied " +
                    "FROM frauenhaus.stichwort_person " +
                    "WHERE stichwort = '" + stichwortNeu + "') ";
            CDataManager.getInstance().getStatement().execute(SQLString);
            // Alte Stichwortzuordnung l�schen
            SQLString = "DELETE FROM frauenhaus.stichwort_person " +
                    "WHERE stichwort IN (" + stichworteAlt + ") " +
                    "AND stichwort != '" + stichwortNeu + "' ";
            CDataManager.getInstance().getStatement().execute(SQLString);
            // Alte Stichw�rter l�schen
            SQLString = "DELETE FROM frauenhaus.stichwort " +
                    "WHERE stichwort IN (" + stichworteAlt + ") " +
                    "AND stichwort != '" + stichwortNeu + "' ";
            CDataManager.getInstance().getStatement().execute(SQLString);
            CDataManager.getInstance().getConnection().commit();
            // Erfolgsmeldung einbauen
            JOptionPane.showMessageDialog(null,
                    "Stichworte erfolgreich zusammengefasst",
                    "Stichworte Zusammenfassen - Info",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to merge Stichworte", e);
            Toolkit.getDefaultToolkit().beep();
            try {
                CDataManager.getInstance().getConnection().rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to rollback Stichworte merge", ex);
            }
        }
    }

    public void set(CProperties p) {
        this.p = p;
    }

}
