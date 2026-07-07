package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Eingang;
import de.frauenhaus.repository.BussgeldRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
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

    public BussgeldReportService(BussgeldRepository bussgelder) {
        this.bussgelder = bussgelder;
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

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            absatz(doc, b.getGericht().getBezeichnung());
            absatz(doc, b.getGericht().getStrasse());
            absatz(doc, b.getGericht().getPlz() + " " + b.getGericht().getOrt());
            absatz(doc, "");
            absatz(doc, "Mannheim, " + DATUM.format(LocalDate.now(ZoneId.systemDefault())));
            absatz(doc, "");
            absatz(doc, "Aktenzeichen: " + (b.getAktenzeichen() == null ? "unbekannt" : b.getAktenzeichen()));
            absatz(doc, "Bußgeld: " + b.getName() + ", " + b.getVorname()
                    + " über " + b.getBetrag().setScale(2, RoundingMode.HALF_UP) + " EUR vom " + DATUM.format(b.getDatum()));
            absatz(doc, "");
            absatz(doc, "Sehr geehrte Damen und Herren,");
            absatz(doc, "hiermit bestätigen wir die folgenden Zahlungseingänge:");
            for (Eingang e : b.getEingaenge()) {
                absatz(doc, DATUM.format(e.getDatum()) + "  " + e.getBetrag().setScale(2, RoundingMode.HALF_UP) + " EUR");
            }
            absatz(doc, "");
            absatz(doc, "Mit freundlichen Grüßen");

            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Bestätigung konnte nicht erzeugt werden", e);
        }
    }

    /** Fügt dem Word-Dokument einen einfachen Textabsatz hinzu. */
    private static void absatz(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().setText(text == null ? "" : text);
    }
}
