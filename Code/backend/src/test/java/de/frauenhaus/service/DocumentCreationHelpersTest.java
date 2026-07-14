package de.frauenhaus.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Nils
 *
 * Testet das Befüllen der echten Word-Vorlagen aus vorlagen/ über ihre Lesezeichen.
 */
class DocumentCreationHelpersTest {

    /**
     * @author Nils
     *
     * Bußgeld-Vorlage: alle Lesezeichen werden mit den Werten befüllt.
     */
    @Test
    void fuelltBussgeldVorlage() throws IOException {
        byte[] doc = DocumentCreationHelpers.fuelleVorlage(Path.of("vorlagen/FHBG.dot"), Map.of(
                "bezeichnung", "Amtsgericht Mannheim",
                "name", "Mustermann",
                "vorname", "Max",
                "aktenzeichen", "XY-123",
                "betrag", "5.000,00 €",
                "datumbetrag", "02.09.2005\t100,00 €\r16.09.2005\t200,00 €",
                "restsumme", "Es stehen noch Zahlungen in Höhe von 4.700,00 € aus."));

        String text = extrahiere(doc);
        assertTrue(text.contains("Amtsgericht Mannheim"));
        assertTrue(text.contains("Strafsache gegen Mustermann, Max"));
        assertTrue(text.contains("AZ: XY-123"));
        assertTrue(text.contains("Bußgeld in Höhe von 5.000,00 €"));
        assertTrue(text.contains("02.09.2005\t100,00 €"));
        assertTrue(text.contains("16.09.2005\t200,00 €"));
        assertTrue(text.contains("Es stehen noch Zahlungen in Höhe von 4.700,00 € aus."));
    }

    /**
     * @author Nils
     *
     * Spendenbescheinigung: Adresse, Betrag, Betrag in Worten und Datum landen in der Vorlage.
     */
    @Test
    void fuelltSpendenbescheinigungsVorlage() throws IOException {
        byte[] doc = DocumentCreationHelpers.fuelleVorlage(Path.of("vorlagen/FHSBGeldspende.dot"), Map.of(
                "vorname", "Max",
                "name", "Mustermann",
                "strasse", "Musterstraße 4711",
                "plz", "1234",
                "ort", "Musterhausen",
                "betrag", "50,00",
                "worte", "*fünfzig*",
                "datum", "01.02.2013"));

        String text = extrahiere(doc);
        assertTrue(text.contains("Max Mustermann"));
        assertTrue(text.contains("Musterstraße 4711"));
        assertTrue(text.contains("1234 Musterhausen"));
        assertTrue(text.contains("Betrag:\t50,00"));
        assertTrue(text.contains("In Worten: *fünfzig* Euro"));
        assertTrue(text.contains("Datum:\t01.02.2013"));
    }

    /**
     * @author Nils
     *
     * Das Lesezeichen datumbetrag sitzt in einem Aufzählungs-Absatz – beim
     * mehrzeiligen Einfügen muss jede Zeile die Aufzählung weiterführen
     * (gleiche Listenzugehörigkeit und gleicher Einzug wie der Vorlagen-Absatz).
     */
    @Test
    void fuehrtAufzaehlungBeiMehrzeiligenWertenWeiter() throws IOException {
        byte[] doc = DocumentCreationHelpers.fuelleVorlage(Path.of("vorlagen/FHBG.dot"), Map.of(
                "datumbetrag", "01.01.2020\t100,00 €\r02.02.2020\t200,00 €\r03.03.2020\t300,00 €"));

        HWPFDocument geladen = new HWPFDocument(new ByteArrayInputStream(doc));
        Range alles = geladen.getRange();
        int zeilen = 0;
        Integer ilfo = null;
        Integer einzug = null;
        for (int i = 0; i < alles.numParagraphs(); i++) {
            Paragraph absatz = alles.getParagraph(i);
            if (!absatz.text().contains(",00 €")) {
                continue;
            }
            zeilen++;
            if (ilfo == null) {
                ilfo = absatz.getIlfo();
                einzug = absatz.getIndentFromLeft();
                assertNotEquals(0, absatz.getIlfo(), "Vorlagen-Absatz muss Teil einer Aufzählung sein");
            } else {
                assertEquals(ilfo, absatz.getIlfo(), "Aufzählung wird nicht weitergeführt: " + absatz.text());
                assertEquals(einzug, absatz.getIndentFromLeft(), "Einzug weicht ab: " + absatz.text());
            }
        }
        assertEquals(3, zeilen, "alle drei Zeilen müssen als eigene Absätze vorhanden sein");
    }

    /**
     * @author Nils
     *
     * Unbekannte Lesezeichen dürfen die Vorlage nicht beschädigen.
     */
    @Test
    void unbekannteLesezeichenBleibenUnangetastet() throws IOException {
        byte[] doc = DocumentCreationHelpers.fuelleVorlage(Path.of("vorlagen/FVSB.dot"),
                Map.of("gibtEsNicht", "Wert"));
        // Vorlage bleibt lesbar und enthält weiterhin ihren Basistext
        assertTrue(extrahiere(doc).contains("Bestätigung"));
    }

    /**
     * @author Nils
     *
     * Liest den kompletten Text des erzeugten Dokuments zum Prüfen aus.
     */
    private static String extrahiere(byte[] doc) throws IOException {
        try (WordExtractor extractor = new WordExtractor(new HWPFDocument(new ByteArrayInputStream(doc)))) {
            return extractor.getText();
        }
    }
}
