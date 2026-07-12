package de.frauenhaus.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Nils
 *
 * Testet das Befüllen der echten Word-Vorlagen aus vorlagen/ über ihre Lesezeichen.
 */
class DocumentCreationHelpersTest {

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

    @Test
    void unbekannteLesezeichenBleibenUnangetastet() throws IOException {
        byte[] doc = DocumentCreationHelpers.fuelleVorlage(Path.of("vorlagen/FVSB.dot"),
                Map.of("gibtEsNicht", "Wert"));
        // Vorlage bleibt lesbar und enthält weiterhin ihren Basistext
        assertTrue(extrahiere(doc).contains("Bestätigung"));
    }

    private static String extrahiere(byte[] doc) throws IOException {
        try (WordExtractor extractor = new WordExtractor(new HWPFDocument(new ByteArrayInputStream(doc)))) {
            return extractor.getText();
        }
    }
}
