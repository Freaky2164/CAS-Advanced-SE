package de.frauenhaus.service;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.repository.MitgliedRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * @author Nils
 *
 * Verteiler, Serienbrief-Adressen und Serienbrief-Generierung, portiert aus
 * CReportVerteiler, CReportSerienbrief, CReportSerienbriefAdressen sowie
 * CCommandBriefFrauenhaus/CCommandBriefFoerderverein (FHBrief.dot/FVBrief.dot).
 * Anders als im Altsystem (Adressliste für Words natives Seriendruck-Feature,
 * Briefkopf einzeln per Word-COM-Lesezeichen) erzeugt {@link #serienbrief} die
 * fertig adressierten Anschreiben direkt als docx (ein Anschreiben je Empfänger).
 */
@Service
@Transactional(readOnly = true)
public class VerteilerService {

    private final MitgliedRepository mitglieder;
    private final WordTemplateService wordTemplate;
    private final JavaMailSender mailSender;
    private final String absender;

    public record VersandErgebnis(int empfaengerAnzahl) { }

    public VerteilerService(MitgliedRepository mitglieder,
                            WordTemplateService wordTemplate,
                            JavaMailSender mailSender,
                            @Value("${spring.mail.absender:no-reply@localhost}") String absender) {
        this.mitglieder = mitglieder;
        this.wordTemplate = wordTemplate;
        this.mailSender = mailSender;
        this.absender = absender;
    }

    /**
     * @author Nils
     *
     * E-Mail-Adressen der Mitglieder mit den gegebenen Stichworten.
     */
    public List<String> emails(Collection<String> stichworte) {
        return mitglieder.findVerteilerEmails(stichworte);
    }

    /**
     * @author Nils
     *
     * Versendet eine Sammel-E-Mail an den Verteiler per BCC.
     */
    public VersandErgebnis versenden(Collection<String> stichworte, String traeger, String betreff, String text) {
        List<String> empfaenger = emails(stichworte);
        if (empfaenger.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Keine E-Mail-Empfänger zu den gewählten Stichworten gefunden");
        }
        if (absender == null || absender.isBlank()) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Mail-Absender ist nicht konfiguriert");
        }

        try {
            MimeMessage nachricht = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(nachricht, false, StandardCharsets.UTF_8.name());
            helper.setFrom(absender);
            helper.setTo(absender);
            helper.setBcc(empfaenger.toArray(String[]::new));
            helper.setSubject(betreff);
            helper.setText(text, false);
            mailSender.send(nachricht);
            return new VersandErgebnis(empfaenger.size());
        } catch (MailException | MessagingException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR,
                    "E-Mail konnte nicht versendet werden: " + fehlermeldung(e), e);
        }
    }

    /**
     * @author Nils
     *
     * Serienbrief-Adressliste als xlsx (Datenquelle für den Seriendruck).
     */
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

    /**
     * @author Nils
     *
     * Serienbrief-Generierung: ein fertig adressiertes Anschreiben je Empfänger im Verteiler
     * (alt: CCommandBriefFrauenhaus/CCommandBriefFoerderverein + FHBrief.dot/FVBrief.dot).
     * Briefkopf, Anschrift und Briefanrede werden vorausgefüllt; der eigentliche Brieftext
     * wird – wie zuvor auch – anschließend in Word ergänzt.
     */
    public byte[] serienbrief(Collection<String> stichworte, String verein) {
        boolean istFoerderverein = "Förderverein".equalsIgnoreCase(verein);

        XWPFDocument doc = wordTemplate.neuesDokument();
        List<Mitglied> empfaenger = mitglieder.findVerteiler(stichworte);
        for (int i = 0; i < empfaenger.size(); i++) {
            if (i > 0) {
                doc.createParagraph().createRun().addBreak(BreakType.PAGE);
            }
            Mitglied m = empfaenger.get(i);

            wordTemplate.absatzFett(doc, istFoerderverein
                    ? "FÖRDERVEREIN MANNHEIMER FRAUENHAUS e. V."
                    : "MANNHEIMER FRAUENHAUS e. V.");
            wordTemplate.absatz(doc, "Postfach 121348, 68064 Mannheim");
            if (!istFoerderverein) {
                wordTemplate.absatz(doc, "Tel.: 0621/744333, Fax.: 0621/744243");
            }
            wordTemplate.leerzeile(doc);

            String zeile1 = ((m.getVorname() == null || m.getVorname().isBlank()) ? "" : m.getVorname() + " ")
                    + m.getName();
            String plzOrt = ((m.getPlz() == null || m.getPlz().isBlank()) ? "" : m.getPlz() + " ")
                    + (m.getOrt() == null ? "" : m.getOrt());
            wordTemplate.adresse(doc, zeile1, m.getName2(), m.getName3(), m.getStrasse(), plzOrt);
            wordTemplate.leerzeile(doc);

            wordTemplate.ortUndDatum(doc, "Mannheim");
            wordTemplate.leerzeile(doc);

            String briefanrede = (m.getBriefanrede() == null || m.getBriefanrede().isBlank())
                    ? "Sehr geehrte Damen und Herren"
                    : m.getBriefanrede();
            wordTemplate.absatz(doc, briefanrede + ",");
            wordTemplate.leerzeile(doc);
            wordTemplate.leerzeile(doc);
            wordTemplate.leerzeile(doc);

            wordTemplate.absatz(doc, "Mit freundlichen Grüßen");
        }
        return wordTemplate.toBytes(doc);
    }

    private static String fehlermeldung(Exception e) {
        Throwable ursache = NestedExceptionUtils.getMostSpecificCause(e);
        String meldung = ursache != null ? ursache.getMessage() : e.getMessage();
        return (meldung == null || meldung.isBlank()) ? e.getClass().getSimpleName() : meldung;
    }
}
