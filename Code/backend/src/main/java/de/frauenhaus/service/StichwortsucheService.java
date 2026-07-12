package de.frauenhaus.service;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.MitgliedRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nils
 *
 * Mitgliedersuche über Stichworte mit xlsx-Export (alt: CReportStichwortSuche).
 * Anders als {@link VerteilerService} durchsucht dies gezielt nach Mitgliedern
 * (statt nur die Adressliste eines bestehenden Verteilers auszugeben) und kann
 * zusätzlich auf Förderverein-/Frauenhaus-Mitglieder eingeschränkt werden.
 */
@Service
@Transactional(readOnly = true)
public class StichwortsucheService {

    private final MitgliedRepository mitglieder;

    public StichwortsucheService(MitgliedRepository mitglieder) {
        this.mitglieder = mitglieder;
    }

    /** Mitglieder, die mindestens eines der gegebenen Stichworte tragen (mit optionalen Filtern). */
    public List<Mitglied> suchen(Collection<String> stichworte, boolean foerderverein, boolean frauenhaus) {
        return mitglieder.findByStichwortSuche(stichworte, foerderverein, frauenhaus);
    }

    /** Suchergebnis als Vorschau-Liste für die UI. */
    public List<MitgliedService.MitgliedResponse> suchenAlsResponses(
            Collection<String> stichworte, boolean foerderverein, boolean frauenhaus) {
        return suchen(stichworte, foerderverein, frauenhaus).stream()
                .map(MitgliedService.MitgliedResponse::of)
                .toList();
    }

    /** Suchergebnis als xlsx-Report (alt: StichwortSuche.xls). */
    public byte[] suchenAlsExcel(Collection<String> stichworte, boolean foerderverein, boolean frauenhaus) {
        Workbook wb = ExcelUtil.neuesWorkbook("StichwortSuche");
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Anrede", "Vorname", "Name", "Name2", "Name3", "Straße", "PLZ", "Ort",
                "E-Mail", "Tel1", "Tel2", "Förderverein", "Frauenhaus", "Vereine", "Stichworte", "Bemerkung");
        for (Mitglied m : suchen(stichworte, foerderverein, frauenhaus)) {
            ExcelUtil.zeile(sheet, line++, Arrays.asList(
                    m.getAnrede(), m.getVorname(), m.getName(), m.getName2(), m.getName3(),
                    m.getStrasse(), m.getPlz(), m.getOrt(), m.getEmail(), m.getTel1(), m.getTel2(),
                    m.isFoerderverein() ? "Ja" : "Nein", m.isFrauenhaus() ? "Ja" : "Nein",
                    m.getVereine().stream().map(Verein::getName).collect(Collectors.joining(", ")),
                    m.getStichworte().stream().map(Stichwort::getName).collect(Collectors.joining(", ")),
                    m.getBemerkung()));
        }
        return ExcelUtil.toBytes(wb);
    }
}
