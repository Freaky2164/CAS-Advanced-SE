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
 * Verteiler-Funktionen über Stichworte: E-Mail-Versand, Serienbrief-Adressen
 * als xlsx und Serienbrief-Generierung als docx mit einem fertig adressierten
 * Anschreiben je Empfänger.
 *
 * @author Robin
 */
@Service
@Transactional(readOnly = true)
public class VerteilerService {

    private final MitgliedRepository mitglieder;
    private final WordTemplateService wordTemplate;
    private final JavaMailSender mailSender;
    private final String absender;

    /**
     * Ergebnis des E-Mail-Versands mit der Anzahl der Empfänger.
     */
    public record VersandErgebnis(int empfaengerAnzahl) { }

    /**
     * Erzeugt den Service mit Repository, Word-Baustein und Mail-Sender.
     *
     * @param mitglieder das Mitglieder-Repository
     * @param wordTemplate der Baustein für Word-Briefe
     * @param mailSender der Mail-Sender für den Verteiler-Versand
     * @param absender die konfigurierte Absenderadresse
     */
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
     * Liefert die E-Mail-Adressen der Mitglieder mit den gegebenen Stichworten.
     *
     * @param stichworte die Namen der Stichworte
     * @return die E-Mail-Adressen ohne Duplikate
     */
    public List<String> emails(Collection<String> stichworte) {
        return mitglieder.findVerteilerEmails(stichworte);
    }

    /**
     * Versendet eine Sammel-E-Mail an den Verteiler per BCC.
     *
     * @param stichworte die Namen der Stichworte
     * @param betreff der Betreff der E-Mail
     * @param text der Nachrichtentext
     * @return das Versandergebnis mit der Empfängeranzahl
     */
    public VersandErgebnis versenden(Collection<String> stichworte, String betreff, String text) {
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
     * Liefert die Serienbrief-Adressliste als xlsx (Datenquelle für den
     * Seriendruck).
     *
     * @param stichworte die Namen der Stichworte
     * @return die Adressliste als xlsx-Datei
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
     * Erzeugt den Serienbrief: ein fertig adressiertes Anschreiben je
     * Empfänger im Verteiler. Briefkopf, Anschrift und Briefanrede werden
     * vorausgefüllt; {@code text} wird als Brieftext eingesetzt
     * (Zeilenumbrüche bleiben erhalten). Ohne Text bleiben Leerzeilen als
     * Platz, um den Brief anschließend in Word zu ergänzen.
     *
     * @param stichworte die Namen der Stichworte
     * @param verein der Kurzname des Trägervereins (bestimmt den Briefkopf)
     * @param text der Brieftext oder {@code null}
     * @return die Anschreiben als docx-Datei
     */
    public byte[] serienbrief(Collection<String> stichworte, String verein, String text) {
        boolean istFoerderverein = "Förderverein".equalsIgnoreCase(verein);

        XWPFDocument doc = wordTemplate.neuesDokument();
        List<Mitglied> empfaenger = mitglieder.findVerteiler(stichworte);
        for (int i = 0; i < empfaenger.size(); i++) {
            if (i > 0) {
                doc.createParagraph().createRun().addBreak(BreakType.PAGE);
            }
            anschreiben(doc, empfaenger.get(i), istFoerderverein, text);
        }
        return wordTemplate.toBytes(doc);
    }

    /**
     * Baut ein einzelnes Anschreiben aus Briefkopf, Anschrift, Datumszeile,
     * Briefanrede, Brieftext und Grußformel auf.
     */
    private void anschreiben(XWPFDocument doc, Mitglied m, boolean istFoerderverein, String text) {
        briefkopf(doc, istFoerderverein);
        anschrift(doc, m);
        wordTemplate.ortUndDatum(doc, "Mannheim");
        wordTemplate.leerzeile(doc);
        wordTemplate.absatz(doc, briefanrede(m) + ",");
        wordTemplate.leerzeile(doc);
        brieftext(doc, text);
        wordTemplate.absatz(doc, "Mit freundlichen Grüßen");
    }

    /**
     * Fügt den Briefkopf des jeweiligen Trägers an.
     */
    private void briefkopf(XWPFDocument doc, boolean istFoerderverein) {
        wordTemplate.absatzFett(doc, istFoerderverein
                ? "FÖRDERVEREIN MANNHEIMER FRAUENHAUS e. V."
                : "MANNHEIMER FRAUENHAUS e. V.");
        wordTemplate.absatz(doc, "Postfach 121348, 68064 Mannheim");
        if (!istFoerderverein) {
            wordTemplate.absatz(doc, "Tel.: 0621/744333, Fax.: 0621/744243");
        }
        wordTemplate.leerzeile(doc);
    }

    /**
     * Fügt den Anschriftblock des Empfängers an.
     */
    private void anschrift(XWPFDocument doc, Mitglied m) {
        String zeile1 = ((m.getVorname() == null || m.getVorname().isBlank()) ? "" : m.getVorname() + " ")
                + m.getName();
        String plzOrt = ((m.getPlz() == null || m.getPlz().isBlank()) ? "" : m.getPlz() + " ")
                + (m.getOrt() == null ? "" : m.getOrt());
        wordTemplate.adresse(doc, zeile1, m.getName2(), m.getName3(), m.getStrasse(), plzOrt);
        wordTemplate.leerzeile(doc);
    }

    /**
     * Liefert die Briefanrede des Mitglieds oder die neutrale Standardanrede.
     */
    private static String briefanrede(Mitglied m) {
        return (m.getBriefanrede() == null || m.getBriefanrede().isBlank())
                ? "Sehr geehrte Damen und Herren"
                : m.getBriefanrede();
    }

    /**
     * Fügt den Brieftext zeilenweise an; ohne Text bleiben Leerzeilen als
     * Platz zum späteren Ergänzen in Word.
     */
    private void brieftext(XWPFDocument doc, String text) {
        if (text == null || text.isBlank()) {
            wordTemplate.leerzeile(doc);
            wordTemplate.leerzeile(doc);
            return;
        }
        for (String zeile : text.split("\n", -1)) {
            if (zeile.isBlank()) {
                wordTemplate.leerzeile(doc);
            } else {
                wordTemplate.absatz(doc, zeile.stripTrailing());
            }
        }
        wordTemplate.leerzeile(doc);
    }

    /**
     * Ermittelt die aussagekräftigste Fehlermeldung aus der Ursachenkette.
     */
    private static String fehlermeldung(Exception e) {
        String meldung = NestedExceptionUtils.getMostSpecificCause(e).getMessage();
        return (meldung == null || meldung.isBlank()) ? e.getClass().getSimpleName() : meldung;
    }
}
