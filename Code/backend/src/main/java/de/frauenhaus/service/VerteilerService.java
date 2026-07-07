package de.frauenhaus.service;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.repository.MitgliedRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author Nils
 *
 * Verteiler und Serienbrief-Adressen, portiert aus CReportVerteiler,
 * CReportSerienbrief und CReportSerienbriefAdressen.
 * Der Seriendruck selbst passiert im Textprogramm – die Adressliste kommt von hier.
 */
@Service
@Transactional(readOnly = true)
public class VerteilerService {

    private final MitgliedRepository mitglieder;

    public VerteilerService(MitgliedRepository mitglieder) {
        this.mitglieder = mitglieder;
    }

    /** E-Mail-Adressen der Mitglieder mit den gegebenen Stichworten. */
    public List<String> emails(Collection<String> stichworte) {
        return mitglieder.findVerteilerEmails(stichworte);
    }

    /** Serienbrief-Adressliste als xlsx (Datenquelle für den Seriendruck). */
    public byte[] adressen(Collection<String> stichworte) {
        Workbook wb = ExcelUtil.neuesWorkbook("Serienbrief-Adressen");
        Sheet sheet = wb.getSheetAt(0);
        int line = 0;
        ExcelUtil.headerZeile(sheet, ExcelUtil.headerStyle(wb), line++,
                "Anrede", "Vorname", "Name", "Name2", "Name3", "Straße", "PLZ", "Ort", "Briefanrede", "E-Mail");
        for (Mitglied m : mitglieder.findVerteiler(stichworte)) {
            ExcelUtil.zeile(sheet, line++, java.util.Arrays.asList(
                    m.getAnrede(), m.getVorname(), m.getName(), m.getName2(), m.getName3(),
                    m.getStrasse(), m.getPlz(), m.getOrt(), m.getBriefanrede(), m.getEmail()));
        }
        return ExcelUtil.toBytes(wb);
    }
}
