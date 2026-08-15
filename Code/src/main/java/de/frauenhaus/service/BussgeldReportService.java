package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Eingang;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.VereinRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Erzeugt die Bußgeld-Reports (Übersicht und Detail) als xlsx. Die
 * Zahlungsbestätigung an das Gericht erzeugt {@link DocumentCreationService}
 * aus den Word-Vorlagen.
 *
 * @author Robin
 */
@Service
@Transactional(readOnly = true)
public class BussgeldReportService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BussgeldRepository bussgelder;
    private final VereinRepository vereine;

    /**
     * Erzeugt den Service mit den benötigten Repositories.
     *
     * @param bussgelder das Bußgeld-Repository
     * @param vereine das Verein-Repository; liefert die Träger der Übersicht
     */
    public BussgeldReportService(BussgeldRepository bussgelder, VereinRepository vereine) {
        this.bussgelder = bussgelder;
        this.vereine = vereine;
    }

    /**
     * Erstellt die Übersicht: je Träger die Summen der Bußgelder und
     * Zahlungseingänge pro Gericht, durch eine Leerzeile getrennt.
     *
     * <p>Die Träger kommen aus der Stammdatentabelle, damit auch der bei der
     * Datenübernahme angelegte Platzhalter-Träger {@code unbekannt} im Report
     * erscheint und keine Datensätze unsichtbar bleiben.</p>
     *
     * @param von Beginn des Zeitraums (einschließlich)
     * @param bis Ende des Zeitraums (einschließlich)
     * @return die Übersicht als xlsx-Datei
     */
    public byte[] uebersicht(LocalDate von, LocalDate bis) {
        Workbook wb = ExcelUtil.neuesWorkbook("Bußgelder Übersicht");
        Sheet sheet = wb.getSheetAt(0);
        CellStyle header = ExcelUtil.headerStyle(wb);
        int line = 0;

        for (String verein : traeger()) {
            ExcelUtil.headerZeile(sheet, header, line++, verein, "Bußgelder", "Eingänge");
            BigDecimal summeBussgeld = BigDecimal.ZERO;
            BigDecimal summeEingang = BigDecimal.ZERO;
            for (BussgeldRepository.UebersichtZeile z : bussgelder.uebersicht(von, bis, verein)) {
                ExcelUtil.zeile(sheet, line++, List.of(z.getBezeichnung(), z.getBussgelder(), z.getEingaenge()));
                summeBussgeld = summeBussgeld.add(z.getBussgelder());
                summeEingang = summeEingang.add(z.getEingaenge());
            }
            ExcelUtil.zeile(sheet, line++, List.of("Summe " + verein, summeBussgeld, summeEingang));
            line++;
        }
        return ExcelUtil.toBytes(wb);
    }

    /**
     * Erstellt den Detail-Report: je Bußgeld eine Zeile pro Zahlungseingang,
     * der im Zeitraum liegt. Die Spalte "Offen" zeigt den nach allen bislang
     * geleisteten Zahlungen noch offenen Betrag. Die Bußgeld-Nr in der ersten
     * Spalte ist die ID für die Dokumenterstellung.
     *
     * @param von Beginn des Zeitraums (einschließlich)
     * @param bis Ende des Zeitraums (einschließlich)
     * @param verein der Kurzname des Trägervereins
     * @return der Detail-Report als xlsx-Datei
     */
    public byte[] detail(LocalDate von, LocalDate bis, String verein) {
        Workbook wb = ExcelUtil.neuesWorkbook("Bußgelder Detail");
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Bußgeld-Nr", "Datum", "Gericht", "Aktenzeichen", "Name", "Bußgeld", "Offen", "Eingang am", "Eingang");

        for (Bussgeld b : bussgelder.findMitEingaengen(von, bis, verein)) {
            BigDecimal gezahlt = b.getEingaenge().stream()
                    .map(Eingang::getBetrag)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal offen = b.getBetrag().subtract(gezahlt);
            String aktenzeichen = b.getAktenzeichen() == null ? "unbekannt" : b.getAktenzeichen();
            for (Eingang e : b.getEingaenge()) {
                if (e.getDatum().isBefore(von) || e.getDatum().isAfter(bis)) {
                    continue;
                }
                ExcelUtil.zeile(sheet, line++, Arrays.asList(
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

    /**
     * Liefert die Kurznamen aller Träger, nach Kurzname sortiert.
     *
     * @return die Trägernamen für die Übersicht
     */
    private List<String> traeger() {
        return vereine.findAll(Sort.by("name")).stream().map(Verein::getName).toList();
    }
}
