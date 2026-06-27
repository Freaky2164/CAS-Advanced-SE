package frauenhaus;

import compucrash.*;
import de.must.util.WordProcessing;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CCommandBriefFoerderverein extends CCommand implements CReport {

    private static final Logger LOGGER = Logger.getLogger(CCommandBriefFoerderverein.class.getName());
    private final String vorlagen;
    String wert = "";
    private String mitglied = "";

    public CCommandBriefFoerderverein() {
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
    }

    public Object execute(Object parameters) {
        if (getOwner() instanceof CInfoFrame frame) {
            Object o = frame.getAttributeValue("frauenhaus.mitglied.mitglied");
            mitglied = o.toString();
        }
        go();
        return null;
    }

    public void go() {
        String sqlString = "SELECT anrede, vorname, name, name2, name3, strasse, plz, ort, briefanrede "
                + "FROM frauenhaus.mitglied "
                + "WHERE mitglied = ?";
        try {
            File file = new File(vorlagen + "/FVBrief.dot");
            String path = file.getAbsolutePath();
            WordProcessing.createNewDocumentFromTemplate(path);
            try (PreparedStatement pstmt = CDataManager.getInstance().getConnection().prepareStatement(sqlString)) {
                pstmt.setString(1, mitglied);
                try (ResultSet rset = pstmt.executeQuery()) {
                    rset.next();
                    wert = nvl(rset.getString(1));
                    insert("anrede", wert);
                    String name = "";
                    wert = nvl(rset.getString(2));
                    if (!wert.trim().isEmpty()) {
                        name = wert + " ";
                    }
                    wert = nvl(rset.getString(3));
                    name += wert;
                    wert = nvl(rset.getString(4));
                    if (!wert.trim().isEmpty()) {
                        name = name + "\n" + wert;
                    }
                    wert = nvl(rset.getString(5));
                    if (!wert.trim().isEmpty()) {
                        name = name + "\n" + wert;
                    }
                    insert("name", name);
                    wert = nvl(rset.getString(6));
                    insert("strasse", wert);
                    wert = nvl(rset.getString(7));
                    insert("plz", wert);
                    wert = nvl(rset.getString(8));
                    insert("ort", wert);
                    wert = nvl(rset.getString(9));
                    insert("briefanrede", wert);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate Foerderverein letter", e);
        }
    }

    public void insert(String bookmark, String text) {
        WordProcessing.typeTextAtBookmark(bookmark, text);
    }

    public String nvl(String in) {
        if (in == null) return " ";
        if (in.isEmpty()) return " ";
        return in;
    }

    public void set(CProperties p) {
        // no-op
    }
}
