package de.frauenhaus.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Baustein zum Aufbau von Word-Briefen mit POI-XWPF direkt aus den Fachdaten.
 * Serienbrief-Generierung und Spendenquittungs-docx bauen auf diesen
 * Methoden auf.
 *
 * @author Paul
 */
@Service
public class WordTemplateService {

    /** Datumsformat für Briefe (TT.MM.JJJJ). */
    public static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Erzeugt ein neues, leeres Word-Dokument.
     *
     * @return das leere Dokument
     */
    public XWPFDocument neuesDokument() {
        return new XWPFDocument();
    }

    /**
     * Fügt einen einfachen Textabsatz an.
     *
     * @param doc das Dokument
     * @param text der Absatztext ({@code null} ergibt einen leeren Absatz)
     * @return der erzeugte Absatz
     */
    public XWPFParagraph absatz(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().setText(text == null ? "" : text);
        return p;
    }

    /**
     * Fügt einen fett hervorgehobenen Textabsatz an, z.B. eine Betreffzeile.
     *
     * @param doc das Dokument
     * @param text der Absatztext ({@code null} ergibt einen leeren Absatz)
     * @return der erzeugte Absatz
     */
    public XWPFParagraph absatzFett(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setText(text == null ? "" : text);
        return p;
    }

    /**
     * Fügt einen rechtsbündigen Textabsatz an, z.B. eine Ort/Datum-Zeile.
     *
     * @param doc das Dokument
     * @param text der Absatztext
     * @return der erzeugte Absatz
     */
    public XWPFParagraph absatzRechts(XWPFDocument doc, String text) {
        XWPFParagraph p = absatz(doc, text);
        p.setAlignment(ParagraphAlignment.RIGHT);
        return p;
    }

    /**
     * Fügt eine Leerzeile zur optischen Gliederung an.
     *
     * @param doc das Dokument
     */
    public void leerzeile(XWPFDocument doc) {
        doc.createParagraph();
    }

    /**
     * Fügt einen mehrzeiligen Adress- oder Absenderblock an, eine Zeile pro
     * Argument; leere und {@code null}-Zeilen werden übersprungen.
     *
     * @param doc das Dokument
     * @param zeilen die Adresszeilen
     */
    public void adresse(XWPFDocument doc, String... zeilen) {
        for (String zeile : zeilen) {
            if (zeile != null && !zeile.isBlank()) {
                absatz(doc, zeile);
            }
        }
    }

    /**
     * Fügt eine "Ort, TT.MM.JJJJ"-Zeile mit dem heutigen Datum an.
     *
     * @param doc das Dokument
     * @param ort der Ort
     */
    public void ortUndDatum(XWPFDocument doc, String ort) {
        absatzRechts(doc, ort + ", " + DATUM.format(LocalDate.now(ZoneId.systemDefault())));
    }

    /**
     * Serialisiert das Dokument als docx-Bytes.
     *
     * @param doc das zu serialisierende Dokument
     * @return der docx-Inhalt als Byte-Array
     */
    public byte[] toBytes(XWPFDocument doc) {
        try (doc; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Word-Dokument konnte nicht erzeugt werden", e);
        }
    }
}
