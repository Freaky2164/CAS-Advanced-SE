package frauenhaus;

import compucrash.*;
import de.must.util.WordProcessing;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;

public class CCommandBestaetigungBussgeld extends CCommand implements CReport {

    private final NumberFormat nf = NumberFormat.getCurrencyInstance();
    private final String vorlagen;
    private CProperties p;

    public CCommandBestaetigungBussgeld() {
        this.vorlagen = CPropertyManager.getInstance().getProperty("vorlagen");
    }

    public Object execute(Object parameters) {
        if (getOwner() instanceof CInfoFrame frame) {
            Object o = frame.getAttributeValue("frauenhaus.bussgeld.bussgeld");
            String bussgeld = o.toString();
            o = frame.getAttributeValue("frauenhaus.bussgeld.verein");
            String verein = o.toString();
            go(bussgeld, verein, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        return null;
    }

    public void go() {
        if (p == null) {
            return;
        }
        Object bussgeld = p.get("bussgeld");
        Object verein = p.get("verein");
        if (bussgeld == null || verein == null) {
            return;
        }
        go(bussgeld.toString(), verein.toString(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public void go(String bussgeld, String verein, DateTimeFormatter fmt) {
        File file;
        if (verein.equalsIgnoreCase("F�rderverein")) {
            file = new File(vorlagen + "/FVBG.dot");
        } else {
            file = new File(vorlagen + "/FHBG.dot");
        }

        String path = file.getAbsolutePath();
        try (Statement stmt = CDataManager.getInstance().getStatement()) {
            WordProcessing.createNewDocumentFromTemplate(path);

            String sqlString = "SELECT g.bezeichnung, g.strasse, g.plz, g.ort, b.name, b.vorname, b.aktenzeichen, round(b.betrag,2), getDate() "
                    + "FROM frauenhaus.bussgeld b, frauenhaus.gericht g "
                    + "WHERE b.bussgeld = " + bussgeld + " "
                    + "AND g.gericht = b.gericht ";

            try (ResultSet rset = stmt.executeQuery(sqlString)) {
                rset.next();

                String wert = rset.getString(1);
                insert("bezeichnung", wert);

                wert = nvl(rset.getString(2));
                insert("strasse", wert);

                wert = nvl(rset.getString(3));
                insert("plz", wert);

                wert = nvl(rset.getString(4));
                insert("ort", wert);

                wert = nvl(rset.getString(5));
                insert("name", wert);

                wert = nvl(rset.getString(6));
                insert("vorname", wert);

                wert = nvl(rset.getString(7));
                insert("aktenzeichen", wert);

                double bussbetrag = rset.getDouble(8);
                wert = nf.format(bussbetrag);
                insert("betrag", wert);

                wert = fmt.format(rset.getDate(9).toLocalDate());
                insert("datum", wert);

                double restbetrag = 0;
                double restbetragsumme = 0;
                StringBuilder wertBuilder = new StringBuilder();

                String paymentsSql = "SELECT datum, COALESCE(round(betrag, 2),0.00) FROM frauenhaus.eingang WHERE bussgeld = "
                        + bussgeld + " ORDER BY datum DESC ";
                try (ResultSet payments = stmt.executeQuery(paymentsSql)) {
                    while (payments.next()) {
                        String datum = fmt.format(payments.getDate(1).toLocalDate());
                        restbetrag = payments.getDouble(2);
                        String betrag = nf.format(restbetrag);
                        wertBuilder.append(datum).append("\t").append(betrag).append("\n");
                        restbetragsumme += restbetrag;
                    }
                }

                insert("datumbetrag", wertBuilder.toString());

                double rest = bussbetrag - restbetragsumme;
                if (rest <= 0) {
                    wert = "Das Bu�geld ist damit vollst�ndig bezahlt.";
                } else if (restbetragsumme == 0) {
                    wert = "Es wurden noch keine Zahlungen geleistet.";
                } else {
                    wert = "Es stehen noch Zahlungen in H�he von: " + nf.format(rest) + " aus.";
                }
                insert("restsumme", wert);
                WordProcessing.exec();
            }
        } catch (SQLException e) {
            CMessage.print(e);
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
        this.p = p;
    }
}
