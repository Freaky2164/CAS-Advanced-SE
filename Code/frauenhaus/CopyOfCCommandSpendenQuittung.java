package frauenhaus;

import java.awt.Toolkit;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import compucrash.CCommand;
import compucrash.CDataManager;
import compucrash.CInfoFrame;
import compucrash.CProperties;
import compucrash.CPropertyManager;
import compucrash.CReport;

import de.must.util.WordProcessing;

public class CopyOfCCommandSpendenQuittung extends CCommand implements CReport {
    
    private CProperties p;
    private NumberFormat nf = NumberFormat.getCurrencyInstance();
    private String reports;
    private String vorlagen;
    private String excel;
    private Statement stmt;
    private String SQLString = "";
    private ResultSet rset;
    private String bescheinigung = "";
    private String spende = "";
    private String datum = "";
    private String verein = "";
    private String wert = "";
    private String betrag = "";
    private DateTimeFormatter fmt;
    private String spendenart = "";
    private String spendentyp;
    private int spendentypInt = 0;
    private static final int MITGLIEDSBEITRAG = 1;
    private static final int SACHSPENDE = 2;
    private static final int GELDSPENDE_DAUER = 3;
    private static final int GELDSPENDE_EINMAL = 4;
    private String empfaenger = "";
    private String datei = "";
    
    public CopyOfCCommandSpendenQuittung() {
        this.reports = CPropertyManager.getInstance().getProperty("reports");
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
        this.excel = CPropertyManager.getInstance().getProperty("excel");
    }
    
    public Object execute(Object parameters) {
        if (owner instanceof CInfoFrame) {
            CInfoFrame frame = (CInfoFrame)owner;
            Object o = frame.getAttributeValue("frauenhaus.spende.spende");
            spende = o.toString();
            o = frame.getAttributeValue("frauenhaus.spende.verein");
            verein = o.toString();
            fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            o = frame.getAttributeValue("frauenhaus.spende.spendenart");
            spendenart = o.toString();
        }
        go();
        return null;
    }
    
    public void go() {
        try {
            SQLString = "SELECT spendentyp FROM frauenhaus.spendenart WHERE spendenart = '" + spendenart + "'";
            stmt = CDataManager.getInstance().getStatement();
            rset = stmt.executeQuery(SQLString);
            rset.next();
            spendentyp = rset.getString(1);
            if (spendentyp.equalsIgnoreCase("Mitgliedsbeitrag")) {
                spendentypInt = MITGLIEDSBEITRAG;
            } else if (spendentyp.equalsIgnoreCase("Sachspende")) {
                spendentypInt = SACHSPENDE;
            } else if (spendentyp.equalsIgnoreCase("Geldspende dauer")) {
                spendentypInt = GELDSPENDE_DAUER;
            } else {
                spendentypInt = GELDSPENDE_EINMAL;
            }
            File file;
            
            if (verein.equalsIgnoreCase("Förderverein")) {
                empfaenger = "FV";
                // Förderverein darf keine Sachspenden quittieren!
                if (spendentypInt == SACHSPENDE) {
                    Toolkit.getDefaultToolkit().beep();                    
                    return;
                }
            } else {
                empfaenger = "FH";
            }
            SQLString = "SELECT m.vorname, m.name, m.strasse, m.plz, m.ort, s.datum, s.betrag " +
            "FROM frauenhaus.spende s, frauenhaus.mitglied m " +
            "WHERE s.spende = " + spende + " " +
            "AND s.mitglied = m.mitglied ";            
            switch (spendentypInt) {
            case SACHSPENDE:
                bescheinigung = "Sachspende";
                datei = empfaenger + "SBSachspende.dot";
                break;
            case GELDSPENDE_DAUER:
                bescheinigung = "Geldspende";
                datei = empfaenger + "SBDauerspende.dot";
                SQLString = "SELECT m.vorname, m.name, m.strasse, m.plz, m.ort, YEAR(s.datum), " +
        		"(SELECT coalesce(SUM(betrag),0.0) " +
        				"FROM frauenhaus.spende s2, frauenhaus.spendenart a " +
        				"WHERE a.spendenart = s2.spendenart " +
        				"AND a.spendentyp = 'Geldspende dauer' " +
        				"AND s2.mitglied = s.mitglied " +
        				"AND YEAR(s2.datum) = YEAR(s.datum)) AS summe, m.mitglied " +
        		"FROM frauenhaus.spende s, frauenhaus.mitglied m " +
        		"WHERE s.spende = " + spende + " " +
        		"AND s.mitglied = m.mitglied ";                
                break;
            case MITGLIEDSBEITRAG:
                bescheinigung = "Mitgliedsbeitrag";
                datei = empfaenger + "SBMitgliedsbeitrag.dot";
                SQLString = "SELECT m.vorname, m.name, m.strasse, m.plz, m.ort, YEAR(s.datum), " +
                		"(SELECT SUM(betrag) " +
                		"FROM frauenhaus.spende s2, frauenhaus.spendenart a " +
                		"WHERE a.spendenart = s2.spendenart " +
                		"AND a.spendentyp = 'Mitgliedsbeitrag' " +
                		"AND s2.mitglied = s.mitglied " +
                		"AND YEAR(s2.datum) = YEAR(s.datum)) AS summe " +
                "FROM frauenhaus.spende s, frauenhaus.mitglied m " +
                "WHERE s.spende = " + spende + " " +
                "AND s.mitglied = m.mitglied ";                
                break;
            default:
                bescheinigung = "Geldspende";
            datei = empfaenger + "SBGeldspende.dot";
            }
            System.out.println(SQLString);
            file = new File(vorlagen + "/" + datei);

            String path = file.getAbsolutePath();
            WordProcessing.createNewDocumentFromTemplate(path);
            stmt = CDataManager.getInstance().getStatement();
            rset = stmt.executeQuery(SQLString);
            rset.next();
            //		bescheinigung
            insert("bescheinigung",bescheinigung);
            //		vorname
            wert = rset.getString(1);
            insert("vorname",wert);
            //		name
            wert = rset.getString(2);
            insert("name",wert);
            //		strasse
            wert = nvl(rset.getString(3));
            insert("strasse",wert);
            //		plz
            wert = nvl(rset.getString(4));
            insert("plz",wert);
            //		ort
            wert = nvl(rset.getString(5));
            insert("ort",wert);
            //		datum
            String jahr = "";
            if (spendentypInt == GELDSPENDE_DAUER || spendentypInt == MITGLIEDSBEITRAG) {
                wert = rset.getString(6);
                jahr = wert;
            } else {
                wert = fmt.format(rset.getDate(6).toLocalDate());

            }
            insert("datum", wert);
            //		betrag
            double betrag = rset.getDouble(7);
            wert = nf.format(betrag);
            insert("betrag",wert);
            //		worte
            char[] ziffer = wert.toCharArray();
            wert = "";
            for (int i = 0; i < ziffer.length; i++) {
                switch (ziffer[i]) {
                case '1':
                    wert += "Eins - ";
                    break;
                case '2':
                    wert += "Zwei - ";
                    break;
                case '3':
                    wert += "Drei - ";
                    break;
                case '4':
                    wert += "Vier - ";
                    break;
                case '5':
                    wert += "Fünf - ";
                    break;
                case '6':
                    wert += "Sechs - ";
                    break;
                case '7':
                    wert += "Sieben - ";
                    break;
                case '8':
                    wert += "Acht - ";
                    break;
                case '9':
                    wert += "Neun - ";
                    break;
                case '0':
                    wert += "Null - ";
                    break;
                case ',':
                    wert += "Komma - ";
                    break;
                }
            }
            wert = wert.substring(0,wert.length() - 2);
            insert("worte", wert);
            if (spendentypInt == GELDSPENDE_DAUER) {
                String mitglied = rset.getString(8);
                SQLString = "SELECT datum, betrag " +
        				"FROM frauenhaus.spende s2, frauenhaus.spendenart a " +
        				"WHERE a.spendenart = s2.spendenart " +
        				"AND a.spendentyp = 'Geldspende dauer' " +
        				"AND s2.mitglied = " + mitglied + " " +
        				"AND YEAR(s2.datum) = " + jahr + " ORDER BY s2.datum";                
                try {
                    stmt = CDataManager.getInstance().getStatement();
                    rset = stmt.executeQuery(SQLString);
                    wert = "";
                    while(rset.next()) {
                        wert += fmt.format(rset.getDate(1).toLocalDate()) + "\t" + nf.format(rset.getDouble(2)) + "\n";
                    }
                    insert("einzelbetrag", wert);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
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
