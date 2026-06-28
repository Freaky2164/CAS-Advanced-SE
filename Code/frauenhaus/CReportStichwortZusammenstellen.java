package frauenhaus;

import compucrash.*;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CReportStichwortZusammenstellen extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CReportStichwortZusammenstellen.class.getName());
    private CProperties p;

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
            CReportSerienbrief.collectSelectedValuesHelper(stichworteAlt, p);
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
            String sqlString = "INSERT INTO frauenhaus.stichwort_person " +
                    "SELECT DISTINCT mitglied, '" + stichwortNeu + "' " +
                    "FROM frauenhaus.mitglied m " +
                    "WHERE m.mitglied IN (SELECT mitglied " +
                    "FROM frauenhaus.stichwort_person " +
                    "WHERE stichwort IN (" + stichworteAlt + ")) " +
                    "AND m.mitglied NOT IN (SELECT mitglied " +
                    "FROM frauenhaus.stichwort_person " +
                    "WHERE stichwort = '" + stichwortNeu + "') ";
            CDataManager.getInstance().getStatement().execute(sqlString);
            CDataManager.getInstance().getConnection().commit();
            // Erfolgsmeldung einbauen
            JOptionPane.showMessageDialog(null,
                    "Stichworte erfolgreich zusammengstellt",
                    "Stichworte Zusammenstellen - Info",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to consolidate Stichworte", e);
            Toolkit.getDefaultToolkit().beep();
            try {
                CDataManager.getInstance().getConnection().rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to rollback Stichworte consolidation", ex);
            }
        }
    }

    public void set(CProperties p) {
        this.p = p;
    }

}
