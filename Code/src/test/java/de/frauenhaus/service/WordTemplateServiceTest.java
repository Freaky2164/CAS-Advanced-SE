package de.frauenhaus.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests der Word-Bausteine für programmatisch erzeugte Briefe.
 *
 * @author Robin
 */
class WordTemplateServiceTest {

    private final WordTemplateService service = new WordTemplateService();

    @Nested
    @DisplayName("absatz")
    class Absatz {

        @Test
        @DisplayName("null ergibt einen leeren Absatz statt einer NullPointerException")
        void absatz_null_ergibtLeerenAbsatz() throws IOException
        {
            try (XWPFDocument doc = service.neuesDokument()) {
                assertThat(service.absatz(doc, null).getText()).isEmpty();
            }
        }

        @Test
        @DisplayName("Text wird übernommen")
        void absatz_text_wirdUebernommen() throws IOException
        {
            try (XWPFDocument doc = service.neuesDokument()) {
                assertThat(service.absatz(doc, "Guten Tag").getText()).isEqualTo("Guten Tag");
            }
        }

        @Test
        @DisplayName("absatzFett setzt die Fett-Auszeichnung")
        void absatzFett_setztBold() throws IOException
        {
            try (XWPFDocument doc = service.neuesDokument()) {
                assertThat(service.absatzFett(doc, "Betreff").getRuns().getFirst().isBold()).isTrue();
            }
        }

        @Test
        @DisplayName("absatzRechts richtet rechtsbündig aus")
        void absatzRechts_setztAusrichtung() throws IOException
        {
            try (XWPFDocument doc = service.neuesDokument()) {
                assertThat(service.absatzRechts(doc, "rechts").getAlignment())
                        .isEqualTo(ParagraphAlignment.RIGHT);
            }
        }
    }

    @Nested
    @DisplayName("adresse")
    class Adresse {

        @Test
        @DisplayName("leere und null-Zeilen werden übersprungen")
        void adresse_leereZeilen_werdenUebersprungen() throws IOException
        {
            try (XWPFDocument doc = service.neuesDokument()) {
                service.adresse(doc, "Sabine Bergmann", null, "  ", "Ahornweg 4", "70173 Stuttgart");

                assertThat(doc.getParagraphs()).hasSize(3)
                        .extracting(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                        .containsExactly("Sabine Bergmann", "Ahornweg 4", "70173 Stuttgart");
            }
        }
    }

    @Nested
    @DisplayName("ortUndDatum")
    class OrtUndDatum {

        @Test
        @DisplayName("schreibt Ort und heutiges Datum rechtsbündig")
        void ortUndDatum_schreibtOrtUndHeute() throws IOException
        {
            String heute = WordTemplateService.DATUM.format(LocalDate.now(ZoneId.systemDefault()));

            try (XWPFDocument doc = service.neuesDokument()) {
                service.ortUndDatum(doc, "Mannheim");

                assertThat(doc.getParagraphs().getFirst().getText()).isEqualTo("Mannheim, " + heute);
                assertThat(doc.getParagraphs().getFirst().getAlignment()).isEqualTo(ParagraphAlignment.RIGHT);
            }
        }
    }

    @Nested
    @DisplayName("toBytes")
    class ToBytes {

        @Test
        @DisplayName("liefert ein wieder lesbares docx")
        void toBytes_liefertLesbaresDokument() throws IOException
        {
            XWPFDocument doc = service.neuesDokument();
            service.absatz(doc, "Inhalt");

            byte[] docx = service.toBytes(doc);

            assertThat(docx).isNotEmpty();
            try (XWPFDocument gelesen = new XWPFDocument(new ByteArrayInputStream(docx))) {
                assertThat(gelesen.getParagraphs().getFirst().getText()).isEqualTo("Inhalt");
            }
        }
    }
}
