package de.frauenhaus.service;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nils
 *
 * Spendenübersicht und Spendenquittungen,
 * portiert aus CReportSpendenUebersicht, CReportSpendenQuittungen und CCommandSpendenQuittung.
 */
@Service
@Transactional(readOnly = true)
public class SpendenService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SpendeRepository spenden;
    private final MitgliedRepository mitglieder;
    private final WordTemplateService wordTemplate;

    public SpendenService(SpendeRepository spenden, MitgliedRepository mitglieder, WordTemplateService wordTemplate) {
        this.spenden = spenden;
        this.mitglieder = mitglieder;
        this.wordTemplate = wordTemplate;
    }

    /** Alle Spenden eines Jahres, gruppiert nach Träger/Spendentyp/-art. */
    public byte[] uebersicht(int jahr) {
        Workbook wb = ExcelUtil.neuesWorkbook("Spendenübersicht " + jahr);
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Träger", "Spendentyp", "Spendenart", "Name", "Vorname", "Datum", "Betrag");
        for (Spende s : spenden.findUebersicht(jahr)) {
            ExcelUtil.zeile(sheet, line++, java.util.Arrays.asList(
                    s.getVerein().getName(),
                    s.getSpendenart().getSpendentyp(),
                    s.getSpendenart().getName(),
                    s.getMitglied().getName(),
                    s.getMitglied().getVorname(),
                    DATUM.format(s.getDatum()),
                    s.getBetrag()));
        }
        return ExcelUtil.toBytes(wb);
    }

    /**
     * Quittungsdaten zu einer Spende. Bei Dauerspenden werden alle Einzelspenden
     * des Jahres summiert (alt: fillDonationSummary), inkl. Betrag in Worten.
     */
    public byte[] quittung(Long spendeId) {
        Spende spende = spenden.findById(spendeId)
                .orElseThrow(() -> new IllegalArgumentException("Spende " + spendeId + " nicht gefunden"));
        String spendentyp = spende.getSpendenart().getSpendentyp();
        String verein = spende.getVerein().getName();

        Workbook wb = ExcelUtil.neuesWorkbook("Spendenquittung");
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Anrede", "Vorname", "Name", "Name2", "Name3", "Straße", "PLZ", "Ort",
                "Träger", "Spendentyp", "Datum", "Betrag", "Betrag in Worten", "Einzelbeträge", "Bemerkung");

        for (Mitglied m : mitglieder.findBySpende(spendeId)) {
            List<Spende> einzelspenden = "Dauerspende".equalsIgnoreCase(spendentyp)
                    ? spenden.findJahresspenden(m.getId(), spende.getDatum().getYear(), spendentyp, verein)
                    : List.of(spende);

            BigDecimal summe = BigDecimal.ZERO;
            List<String> betraege = new ArrayList<>();
            List<String> bemerkungen = new ArrayList<>();
            for (Spende s : einzelspenden) {
                summe = summe.add(s.getBetrag());
                betraege.add(DATUM.format(s.getDatum()) + "\t" + s.getBetrag().setScale(2));
                if (s.getBemerkung() != null && !s.getBemerkung().isBlank()) {
                    bemerkungen.add(s.getBemerkung().trim());
                }
            }

            ExcelUtil.zeile(sheet, line++, java.util.Arrays.asList(
                    m.getAnrede(), m.getVorname(), m.getName(), m.getName2(), m.getName3(),
                    m.getStrasse(), m.getPlz(), m.getOrt(),
                    verein, spendentyp, DATUM.format(spende.getDatum()),
                    summe, BetragInWorten.von(summe),
                    String.join("\n", betraege), String.join("\n", bemerkungen)));
        }
        return ExcelUtil.toBytes(wb);
    }

    /**
     * Spendenquittung als formatiertes Zuwendungsbestätigungs-Dokument (docx), eine Seite je
     * Empfänger (alt: CCommandSpendenQuittung + SpendenQuittungen.xls-Vorlage). Ersetzt den
     * bisherigen reinen Datenexport durch den tatsächlichen Bestätigungstext im Layout der
     * Frauenhaus-/Förderverein-Vorlagen.
     */
    public byte[] quittungDocx(Long spendeId) {
        Spende spende = spenden.findById(spendeId)
                .orElseThrow(() -> new IllegalArgumentException("Spende " + spendeId + " nicht gefunden"));
        String spendentyp = spende.getSpendenart().getSpendentyp();
        String verein = spende.getVerein().getName();
        boolean istFoerderverein = "Förderverein".equalsIgnoreCase(verein);

        XWPFDocument doc = wordTemplate.neuesDokument();
        List<Mitglied> empfaenger = mitglieder.findBySpende(spendeId);
        for (int i = 0; i < empfaenger.size(); i++) {
            if (i > 0) {
                doc.createParagraph().createRun().addBreak(BreakType.PAGE);
            }
            Mitglied m = empfaenger.get(i);
            List<Spende> einzelspenden = "Dauerspende".equalsIgnoreCase(spendentyp)
                    ? spenden.findJahresspenden(m.getId(), spende.getDatum().getYear(), spendentyp, verein)
                    : List.of(spende);
            BigDecimal summe = BigDecimal.ZERO;
            for (Spende s : einzelspenden) {
                summe = summe.add(s.getBetrag());
            }

            wordTemplate.absatzFett(doc, istFoerderverein
                    ? "FÖRDERVEREIN MANNHEIMER FRAUENHAUS e. V."
                    : "MANNHEIMER FRAUENHAUS e. V.");
            wordTemplate.absatz(doc, "Postfach 121348, 68064 Mannheim");
            if (!istFoerderverein) {
                wordTemplate.absatz(doc, "Tel.: 0621/744333, Fax.: 0621/744243");
            }
            wordTemplate.leerzeile(doc);
            wordTemplate.absatzFett(doc, "Bestätigung über eine Geldzuwendung / Mitgliedsbeitrag");
            wordTemplate.absatz(doc, "im Sinne des § 10b des Einkommensteuergesetzes an eine der in § 5 Abs. 1 Nr. 9 "
                    + "des Körperschaftsteuergesetzes bezeichneten Körperschaften, Personenvereinigungen o. "
                    + "Vermögensmassen.");
            wordTemplate.leerzeile(doc);

            wordTemplate.absatz(doc, "Name und Anschrift des Zuwendenden:");
            wordTemplate.adresse(doc,
                    (m.getAnrede() == null ? "" : m.getAnrede() + " ") + m.getVorname() + " " + m.getName(),
                    m.getStrasse(), (m.getPlz() == null ? "" : m.getPlz() + " ") + m.getOrt());
            wordTemplate.leerzeile(doc);

            wordTemplate.absatz(doc, "Betrag der Zuwendung – in Ziffern: " + summe.setScale(2) + " EUR");
            wordTemplate.absatz(doc, "– in Buchstaben: " + BetragInWorten.von(summe));
            wordTemplate.absatz(doc, "Tag der Zuwendung:");
            for (Spende s : einzelspenden) {
                wordTemplate.absatz(doc, "  " + DATUM.format(s.getDatum()) + "  " + s.getBetrag().setScale(2) + " EUR");
            }
            wordTemplate.leerzeile(doc);

            wordTemplate.absatz(doc, "Es handelt sich um den Verzicht auf Erstattung von Aufwendungen: Nein");
            wordTemplate.leerzeile(doc);

            // Freistellungsbescheid-Angaben sind vereinsseitig statisch (wie zuvor im .xls-Vorlagentext)
            // und müssen bei einem neuen Freistellungsbescheid des Finanzamts hier aktualisiert werden.
            wordTemplate.absatz(doc, istFoerderverein
                    ? "Wir sind wegen Förderung von ausschließlich und unmittelbaren steuerbegünstigten Zwecken im "
                            + "Sinne der §§ 51 ff. Abgabenordnung (AO) nach dem letzten uns zugegangenen "
                            + "Freistellungsbescheid bzw. nach der Anlage zum Körperschaftsteuerbescheid des "
                            + "Finanzamtes Mannheim-Neckarstadt, StNr. 37006/22991, vom 20.04.2011 nach § 5 Abs. 1 "
                            + "Nr. 9 des Körperschaftsteuergesetzes für die Jahre 2008, 2009 und 2010 von der "
                            + "Körperschaftsteuer und nach § 3 Nr. 6 Gewerbesteuergesetzes von der Gewerbesteuer "
                            + "befreit."
                    : "Wir sind wegen Förderung von ausschließlich und unmittelbaren steuerbegünstigten Zwecken im "
                            + "Sinne der §§ 51 ff. Abgabenordnung (AO) nach dem letzten uns zugegangenen "
                            + "Freistellungsbescheid bzw. nach der Anlage zum Körperschaftsteuerbescheid des "
                            + "Finanzamtes Mannheim-Neckarstadt, StNr. 37006/45069, vom 08.10.2012 nach § 5 Abs. 1 "
                            + "Nr. 9 des Körperschaftsteuergesetzes für die Jahre 2009, 2010 und 2011 von der "
                            + "Körperschaftsteuer und nach § 3 Nr. 6 Gewerbesteuergesetzes von der Gewerbesteuer "
                            + "befreit.");
            wordTemplate.leerzeile(doc);

            wordTemplate.absatz(doc, istFoerderverein
                    ? "Es wird bestätigt, dass die Zuwendung nur zur Förderung folgender gemeinnütziger Zwecke "
                            + "verwendet wird: Förderung des Wohlfahrtswesens."
                    : "Es wird bestätigt, dass die Zuwendung nur für mildtätige Zwecke sowie folgende "
                            + "gemeinnützige Zwecke verwendet wird: Förderung des öffentlichen Gesundheitswesens "
                            + "(§ 52 Abs. 2 Satz 1 Nr. 3 AO), Förderung der Jugendhilfe (§ 52 Abs. 2 Satz 1 Nr. 4 "
                            + "AO), Förderung der Erziehung (§ 52 Abs. 2 Satz 1 Nr. 7 AO), Förderung des "
                            + "Wohlfahrtswesens (§ 52 Abs. 2 Satz 1 Nr. 9 AO), Förderung Hilfe für Opfer von "
                            + "Straftaten (§ 52 Abs. 2 Satz 1 Nr. 10 AO).");
            wordTemplate.leerzeile(doc);

            wordTemplate.ortUndDatum(doc, "Mannheim");
            wordTemplate.absatz(doc, "(Ort, Datum und Unterschrift des Zuwendungsempfängers)");
            wordTemplate.leerzeile(doc);

            wordTemplate.absatz(doc, "Hinweis: Wer vorsätzlich oder grob fahrlässig eine unrichtige "
                    + "Zuwendungsbestätigung erstellt oder wer veranlasst, dass Zuwendungen nicht zu den in der "
                    + "Zuwendungsbestätigung angegebenen steuerbegünstigten Zwecken verwendet werden, haftet für "
                    + "die Steuer (§ 10 b Abs. 4 EStG, § 9 Abs. 3 KStG, § 9 Nr. 5 GewStG).");
        }
        return wordTemplate.toBytes(doc);
    }
}
