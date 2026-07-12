package de.frauenhaus.service;

import de.frauenhaus.domain.Spende;
import de.frauenhaus.repository.SpendeRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * @author Nils
 *
 * Spendenübersicht als xlsx, portiert aus CReportSpendenUebersicht.
 * Die Spendenbescheinigungen erzeugt {@link DocumentCreationService}
 * aus den Word-Vorlagen.
 */
@Service
@Transactional(readOnly = true)
public class SpendenService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SpendeRepository spenden;

    public SpendenService(SpendeRepository spenden) {
        this.spenden = spenden;
    }

    /** Alle Spenden eines Jahres, gruppiert nach Träger/Spendentyp/-art. */
    public byte[] uebersicht(int jahr) {
        Workbook wb = ExcelUtil.neuesWorkbook("Spendenübersicht " + jahr);
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        // Spenden-Nr ist die ID für die Dokumenterstellung (/api/reports/spendenquittung/{id})
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Spenden-Nr", "Träger", "Spendentyp", "Spendenart", "Name", "Vorname", "Datum", "Betrag");
        for (Spende s : spenden.findUebersicht(jahr)) {
            ExcelUtil.zeile(sheet, line++, java.util.Arrays.asList(
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
