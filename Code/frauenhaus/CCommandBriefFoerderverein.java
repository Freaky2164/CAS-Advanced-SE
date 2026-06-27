package frauenhaus;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CInfoFrame;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;

import de.must.util.WordProcessing;

public class CCommandBriefFoerderverein extends CCommand implements CReport {
    
    private CProperties p;
    private NumberFormat nf = NumberFormat.getCurrencyInstance();
    private String reports;
    private String vorlagen;
    private String excel;
    private Statement stmt;
    private String SQLString = "";
    private ResultSet rset;
    private String mitglied = "";

    private String empfaenger = "";
    private String datei = "";
    String wert = "";
    
    public CCommandBriefFoerderverein() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }
    
    public Object execute(Object parameters) {
        if (owner instanceof CInfoFrame) {
            CInfoFrame frame = (CInfoFrame)owner;
            Object o = frame.getAttributeValue("frauenhaus.mitglied.mitglied");
            mitglied = o.toString();
        }
        go();
        return null;
    }
    
    public void go() {
        try {
            SQLString = "SELECT anrede, vorname, name, name2, name3, strasse, plz, ort, briefanrede " +
            		"FROM frauenhaus.mitglied " +
            		"WHERE mitglied = '" + mitglied + "'";
            stmt = CDataManager.getInstance().getStatement();
            rset = stmt.executeQuery(SQLString);
            File file;
           
            file = new File(vorlagen + "/FVBrief.dot");
            String path = file.getAbsolutePath();
            WordProcessing.createNewDocumentFromTemplate(path);
            stmt = CDataManager.getInstance().getStatement();
            rset = stmt.executeQuery(SQLString);
            rset.next();
            //		anrede
            wert = nvl(rset.getString(1));
            insert("anrede",wert);
            //		vorname
            String name = "";
            wert = nvl(rset.getString(2));
            if (wert.trim().length() > 0) {
                name = wert + " ";
            }
            //		name
            wert = nvl(rset.getString(3));
            	name += wert;
            //		name2
            wert = nvl(rset.getString(4));
            if (wert.trim().length() > 0) {
                name = name + "\n" + wert;
            }
            //		name3
            wert = nvl(rset.getString(5));
            if (wert.trim().length() > 0) {
                name = name + "\n" + wert;
            }
            insert("name",name);
            //		strasse
            wert = nvl(rset.getString(6));
            insert("strasse",wert);
            //		plz
            wert = nvl(rset.getString(7));
            insert("plz",wert);
            //		ort
            wert = nvl(rset.getString(8));
            insert("ort",wert);
            //		briefanrede
            wert = nvl(rset.getString(9));
            insert("briefanrede",wert);
            
            WordProcessing.exec();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void insert (String bookmark, String text) {
        WordProcessing.typeTextAtBookmark(bookmark,text);
    }
    
    public String nvl(String in) {
        if (in == null) return " ";
        if (in.length() == 0) return " ";
        return in;
    }
    
    public void set(CProperties p) {
        this.p = p;
    }
    
}
