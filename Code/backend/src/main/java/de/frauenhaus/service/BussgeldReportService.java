package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Eingang;
import de.frauenhaus.repository.BussgeldRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Nils
 *
 * Bußgeld-Reports, portiert aus CReportBussgeldUebersicht, CReportBussgeldDetail
 * und CCommandBestaetigungBussgeld (Word-COM → docx via POI).
 */
@Service
@Transactional(readOnly = true)
public class BussgeldReportService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<String> VEREINE = List.of("Förderverein", "Frauenhaus");

    private final BussgeldRepository bussgelder;
    private final WordTemplateService wordTemplate;

    public BussgeldReportService(BussgeldRepository bussgelder, WordTemplateService wordTemplate) {
        this.bussgelder = bussgelder;
        this.wordTemplate = wordTemplate;
    }

    /** Übersicht: je Träger die Summen pro Gericht (Bußgelder vs. Zahlungseingänge). */
    public byte[] uebersicht(LocalDate von, LocalDate bis) {
        Workbook wb = ExcelUtil.neuesWorkbook("Bußgelder Übersicht");
        Sheet sheet = wb.getSheetAt(0);
        CellStyle header = ExcelUtil.headerStyle(wb);
        int line = 0;

        for (String verein : VEREINE) {
            ExcelUtil.headerZeile(sheet, header, line++, verein, "Bußgelder", "Eingänge");
            BigDecimal summeBussgeld = BigDecimal.ZERO;
            BigDecimal summeEingang = BigDecimal.ZERO;
            for (BussgeldRepository.UebersichtZeile z : bussgelder.uebersicht(von, bis, verein)) {
                ExcelUtil.zeile(sheet, line++, List.of(z.getBezeichnung(), z.getBussgelder(), z.getEingaenge()));
                summeBussgeld = summeBussgeld.add(z.getBussgelder());
                summeEingang = summeEingang.add(z.getEingaenge());
            }
            ExcelUtil.zeile(sheet, line++, List.of("Summe " + verein, summeBussgeld, summeEingang));
            line++; // Leerzeile zwischen den Trägern
        }
        return ExcelUtil.toBytes(wb);
    }

    /** Detail: Bußgelder mit allen Zahlungseingängen im Zeitraum. */
    public byte[] detail(LocalDate von, LocalDate bis, String verein) {
        Workbook wb = ExcelUtil.neuesWorkbook("Bußgelder Detail");
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Datum", "Gericht", "Aktenzeichen", "Name", "Bußgeld", "Offen", "Eingang am", "Eingang");

        for (Bussgeld b : bussgelder.findMitEingaengen(von, bis, verein)) {
            BigDecimal gezahlt = b.getEingaenge().stream()
                    .map(Eingang::getBetrag)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal offen = b.getBetrag().subtract(gezahlt);
            String aktenzeichen = b.getAktenzeichen() == null ? "unbekannt" : b.getAktenzeichen();
            for (Eingang e : b.getEingaenge()) {
                ExcelUtil.zeile(sheet, line++, java.util.Arrays.asList(
                        DATUM.format(b.getDatum()),
                        b.getGericht().getBezeichnung(),
                        aktenzeichen,
                        b.getName() + ", " + b.getVorname(),
                        b.getBetrag(),
                        offen,
                        DATUM.format(e.getDatum()),
                        e.getBetrag()));
            }
        }
        return ExcelUtil.toBytes(wb);
    }

    /** Zahlungsbestätigung an das Gericht als docx (alt: FHBG.dot + Word-COM). */
    public byte[] bestaetigung(Long bussgeldId) {
        Bussgeld b = bussgelder.findById(bussgeldId)
                .orElseThrow(() -> new IllegalArgumentException("Bußgeld " + bussgeldId + " nicht gefunden"));

        XWPFDocument doc = wordTemplate.neuesDokument();
        wordTemplate.adresse(doc, b.getGericht().getBezeichnung(), b.getGericht().getStrasse(),
                b.getGericht().getPlz() + " " + b.getGericht().getOrt());
        wordTemplate.leerzeile(doc);
        wordTemplate.ortUndDatum(doc, "Mannheim");
        wordTemplate.leerzeile(doc);
        wordTemplate.absatz(doc, "Aktenzeichen: " + (b.getAktenzeichen() == null ? "unbekannt" : b.getAktenzeichen()));
        wordTemplate.absatz(doc, "Bußgeld: " + b.getName() + ", " + b.getVorname()
                + " über " + b.getBetrag().setScale(2, RoundingMode.HALF_UP) + " EUR vom " + DATUM.format(b.getDatum()));
        wordTemplate.leerzeile(doc);
        wordTemplate.absatz(doc, "Sehr geehrte Damen und Herren,");
        wordTemplate.absatz(doc, "hiermit bestätigen wir die folgenden Zahlungseingänge:");
        for (Eingang e : b.getEingaenge()) {
            wordTemplate.absatz(doc, DATUM.format(e.getDatum()) + "  " + e.getBetrag().setScale(2, RoundingMode.HALF_UP) + " EUR");
        }
        wordTemplate.leerzeile(doc);
        wordTemplate.absatz(doc, "Mit freundlichen Grüßen");

        return wordTemplate.toBytes(doc);
    }
}
