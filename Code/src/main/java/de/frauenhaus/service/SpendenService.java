package de.frauenhaus.service;

import de.frauenhaus.domain.Spende;
import de.frauenhaus.repository.SpendeRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Erzeugt die Spendenübersicht eines Jahres als xlsx-Report.
 *
 * @author Robin
 */
@Service
@Transactional(readOnly = true)
public class SpendenService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SpendeRepository spenden;

    /**
     * Erzeugt den Service mit dem Spenden-Repository.
     *
     * @param spenden das Spenden-Repository
     */
    public SpendenService(SpendeRepository spenden) {
        this.spenden = spenden;
    }

    /**
     * Erstellt die Übersicht aller Spenden eines Jahres, gruppiert nach
     * Träger, Spendentyp und Spendenart. Die Spenden-Nr in der ersten Spalte
     * ist die ID für die Dokumenterstellung.
     *
     * @param jahr das Kalenderjahr
     * @return die Übersicht als xlsx-Datei
     */
    public byte[] uebersicht(int jahr) {
        Workbook wb = ExcelUtil.neuesWorkbook("Spendenübersicht " + jahr);
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Spenden-Nr", "Träger", "Spendentyp", "Spendenart", "Name", "Vorname", "Datum", "Betrag");
        for (Spende s : spenden.findUebersicht(jahr)) {
            ExcelUtil.zeile(sheet, line++, Arrays.asList(
                    s.getId(),
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
}
