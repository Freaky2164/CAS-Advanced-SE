package frauenhaus;

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

public class CCommandBestaetigungBussgeld extends CCommand implements CReport {

	private CProperties p;
	private NumberFormat nf = NumberFormat.getCurrencyInstance();
	private String reports;
	private String vorlagen;
	private String excel;
	private Statement stmt;
	private String SQLString = "";
	private ResultSet rset;
	private String bussgeld = "";
	private String datum = "";
	private String verein = "";
	private String wert = "";
	private String betrag = "";
	private DateTimeFormatter fmt;
	public CCommandBestaetigungBussgeld() {
		this.reports = CPropertyManager.getInstance().getProperty("reports");
		this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
		this.excel = CPropertyManager.getInstance().getProperty("excel");
	}

	public Object execute(Object parameters) {
	    if (owner instanceof CInfoFrame) {
	        CInfoFrame frame = (CInfoFrame)owner;
	        Object o = frame.getAttributeValue("frauenhaus.bussgeld.bussgeld");
	        bussgeld = o.toString();
	        o = frame.getAttributeValue("frauenhaus.bussgeld.verein");
	        verein = o.toString();
			fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	    }
	    go();
		return null;
	}

	public void go() {
	    File file;
	    if (verein.equalsIgnoreCase("Förderverein")) {
	        file = new File(vorlagen + "/FVBG.dot");
	        
	    } else {
	        file = new File(vorlagen + "/FHBG.dot");
	    }
        String path = file.getAbsolutePath();
        try {
            stmt = CDataManager.getInstance().getStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
	    WordProcessing.createNewDocumentFromTemplate(path);
	    SQLString = "SELECT g.bezeichnung, g.strasse, g.plz, g.ort, b.name, b.vorname, b.aktenzeichen, round(b.betrag,2), getDate() " +
	    		"FROM frauenhaus.bussgeld b, frauenhaus.gericht g " +
	    		"WHERE b.bussgeld = " + bussgeld + " " +
	    		"AND g.gericht = b.gericht ";
	    try {
	        rset = stmt.executeQuery(SQLString);
	        rset.next();
//	    bezeichnung
		    wert = rset.getString(1);
		    insert("bezeichnung",wert);
//		strasse
		    wert = nvl(rset.getString(2));
		    insert("strasse",wert);
//		plz
		    wert = nvl(rset.getString(3));
		    insert("plz",wert);
//		ort
		    wert = nvl(rset.getString(4));
		    insert("ort",wert);
//		name
		    wert = nvl(rset.getString(5));
		    insert("name",wert);
//		vorname
		    wert = nvl(rset.getString(6));
		    insert("vorname",wert);
//		aktenzeichen
		    wert = nvl(rset.getString(7));
		    insert("aktenzeichen",wert);
//		betrag
			double bussbetrag = rset.getDouble(8);
		    wert = nf.format(bussbetrag);
		    insert("betrag",wert);
		// datum
		wert = fmt.format(rset.getDate(9).toLocalDate());
			insert("datum", wert);
// 		einzahlungen
			double restbetrag = 0;
			double restbetragsumme = 0;
	        wert = "";
	        rset.close();
	        SQLString = "SELECT datum, COALESCE(round(betrag, 2),0.00) FROM frauenhaus.eingang WHERE bussgeld = " 
		        + bussgeld + " ORDER BY datum DESC ";
		        rset = stmt.executeQuery(SQLString);
		        while(rset.next()) {
					datum = fmt.format(rset.getDate(1).toLocalDate());
				    restbetrag = rset.getDouble(2);
				    betrag = nf.format(restbetrag);
		            wert += datum + "\t" + betrag + "\n";  
		            restbetragsumme += restbetrag;
		        }
			    insert("datumbetrag",wert);
//		restsumme
			double rest = bussbetrag - restbetragsumme;
			if (rest <= 0) {
			    wert = "Das Bußgeld ist damit vollständig bezahlt.";
			} else if (restbetragsumme == 0) {
			    wert = "Es wurden noch keine Zahlungen geleistet.";
			} else {
			    wert = "Es stehen noch Zahlungen in Höhe von: " + nf.format(rest) + " aus.";
			}
			insert("restsumme", wert);
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
