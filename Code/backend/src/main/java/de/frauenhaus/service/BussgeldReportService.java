package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Eingang;
import de.frauenhaus.repository.BussgeldRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Nils
 *
 * Bußgeld-Reports (Übersicht und Detail als xlsx), portiert aus
 * CReportBussgeldUebersicht und CReportBussgeldDetail. Die Zahlungsbestätigung
 * an das Gericht erzeugt {@link DocumentCreationService} aus den Word-Vorlagen.
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
        // Bußgeld-Nr ist die ID für die Dokumenterstellung (/api/reports/bussgeld-bestaetigung/{id})
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Bußgeld-Nr", "Datum", "Gericht", "Aktenzeichen", "Name", "Bußgeld", "Offen", "Eingang am", "Eingang");

        for (Bussgeld b : bussgelder.findMitEingaengen(von, bis, verein)) {
            BigDecimal gezahlt = b.getEingaenge().stream()
                    .map(Eingang::getBetrag)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal offen = b.getBetrag().subtract(gezahlt);
            String aktenzeichen = b.getAktenzeichen() == null ? "unbekannt" : b.getAktenzeichen();
            for (Eingang e : b.getEingaenge()) {
                ExcelUtil.zeile(sheet, line++, java.util.Arrays.asList(
                        b.getId(),
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
}
