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
 * @author Nils
 *
 * Wiederverwendbarer Word-Dokumentenbaustein (ersetzt die Word-COM-Ansteuerung
 * aus {@code de/must/util/WordProcessing.java}). Statt echter .dot/.docx-Vorlagen
 * (im Altsystem in {@code frauenhaus/vorlagen/} abgelegt, aber nicht als
 * bearbeitbare Dateien überliefert) werden die Briefe mit POI-XWPF direkt aus
 * den Fachdaten zusammengesetzt – nach demselben Muster wie zuvor in
 * {@link BussgeldReportService#bestaetigung}. Serienbrief-Generierung und
 * Spendenquittung-docx bauen auf diesem Baustein auf.
 */
@Service
public class WordTemplateService {

    public static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * @author Nils
     *
     * Neues, leeres Word-Dokument.
     */
    public XWPFDocument neuesDokument() {
        return new XWPFDocument();
    }

    /**
     * @author Nils
     *
     * Einfacher Textabsatz.
     */
    public XWPFParagraph absatz(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().setText(text == null ? "" : text);
        return p;
    }

    /**
     * @author Nils
     *
     * Fett hervorgehobener Textabsatz (z. B. Betreffzeile).
     */
    public XWPFParagraph absatzFett(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setText(text == null ? "" : text);
        return p;
    }

    /**
     * @author Nils
     *
     * Rechtsbündiger Textabsatz (z. B. Ort/Datum-Zeile).
     */
    public XWPFParagraph absatzRechts(XWPFDocument doc, String text) {
        XWPFParagraph p = absatz(doc, text);
        p.setAlignment(ParagraphAlignment.RIGHT);
        return p;
    }

    /**
     * @author Nils
     *
     * Leerzeile zur optischen Gliederung.
     */
    public void leerzeile(XWPFDocument doc) {
        doc.createParagraph();
    }

    /**
     * @author Nils
     *
     * Mehrzeiliger Adress-/Absenderblock, eine Zeile pro Argument (leere/{@code null}-Zeilen werden übersprungen).
     */
    public void adresse(XWPFDocument doc, String... zeilen) {
        for (String zeile : zeilen) {
            if (zeile != null && !zeile.isBlank()) {
                absatz(doc, zeile);
            }
        }
    }

    /**
     * @author Nils
     *
     * "Ort, TT.MM.JJJJ"-Zeile mit dem heutigen Datum.
     */
    public void ortUndDatum(XWPFDocument doc, String ort) {
        absatzRechts(doc, ort + ", " + DATUM.format(LocalDate.now(ZoneId.systemDefault())));
    }

    /**
     * @author Nils
     *
     * Serialisiert das Dokument als docx-Bytes.
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
