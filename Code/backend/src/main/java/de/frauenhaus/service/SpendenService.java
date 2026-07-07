package de.frauenhaus.service;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

    public SpendenService(SpendeRepository spenden, MitgliedRepository mitglieder) {
        this.spenden = spenden;
        this.mitglieder = mitglieder;
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
}
