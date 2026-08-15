package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.DokumentRepository;
import de.frauenhaus.repository.GerichtRepository;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import de.frauenhaus.repository.VereinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests der Dokument-Anhänge: Validierung von Ziel-Entität, Dateigröße und
 * Dateiname sowie das Entfernen von Pfadanteilen aus dem Upload.
 *
 * @author Nils
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DokumentServiceTest {

    private static final byte[] INHALT = "Testinhalt".getBytes(StandardCharsets.UTF_8);

    @Mock
    private DokumentRepository dokumente;
    @Mock
    private MitgliedRepository mitglieder;
    @Mock
    private VereinRepository vereine;
    @Mock
    private BussgeldRepository bussgelder;
    @Mock
    private SpendeRepository spenden;
    @Mock
    private GerichtRepository gerichte;

    @InjectMocks
    private DokumentService service;

    @Captor
    private ArgumentCaptor<Dokument> gespeichert;

    private void mitgliedExistiert()
    {
        when(mitglieder.existsById(1L)).thenReturn(true);
        when(dokumente.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));
    }

    @Nested
    @DisplayName("hochladen")
    class Hochladen {

        @Test
        @DisplayName("gültiger Upload wird mit Metadaten gespeichert")
        void hochladen_gueltig_speichertDokument()
        {
            mitgliedExistiert();

            DokumentService.DokumentMetadaten metadaten =
                    service.hochladen("MITGLIED", "1", "brief.pdf", MediaType.APPLICATION_PDF_VALUE, INHALT);

            verify(dokumente).save(gespeichert.capture());
            Dokument d = gespeichert.getValue();
            assertThat(d.getEntityTyp()).isEqualTo(Dokument.EntityTyp.MITGLIED);
            assertThat(d.getEntityId()).isEqualTo("1");
            assertThat(d.getDateiname()).isEqualTo("brief.pdf");
            assertThat(d.getGroesse()).isEqualTo(INHALT.length);
            assertThat(d.getHochgeladenVon()).isEqualTo("system");
            assertThat(metadaten.dateiname()).isEqualTo("brief.pdf");
        }

        @Test
        @DisplayName("entityTyp ist unabhängig von Groß-/Kleinschreibung")
        void hochladen_kleingeschriebenerTyp_wirdAkzeptiert()
        {
            mitgliedExistiert();

            service.hochladen("mitglied", "1", "brief.pdf", MediaType.APPLICATION_PDF_VALUE, INHALT);

            verify(dokumente).save(gespeichert.capture());
            assertThat(gespeichert.getValue().getEntityTyp()).isEqualTo(Dokument.EntityTyp.MITGLIED);
        }

        @Test
        @DisplayName("fehlender Content-Type -> application/octet-stream")
        void hochladen_ohneContentType_setztOctetStream()
        {
            mitgliedExistiert();

            service.hochladen("MITGLIED", "1", "brief.pdf", "  ", INHALT);

            verify(dokumente).save(gespeichert.capture());
            assertThat(gespeichert.getValue().getContentType())
                    .isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }

        @ParameterizedTest(name = "entityTyp \"{0}\" -> 400")
        @ValueSource(strings = {"", "   ", "UNSINN", "mitglieder"})
        @DisplayName("ungültiger entityTyp -> 400")
        void hochladen_ungueltigerTyp_wirft400(String typ)
        {
            assertThatThrownBy(() -> service.hochladen(typ, "1", "a.pdf", "application/pdf", INHALT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("nicht-numerische Entity-ID -> 400")
        void hochladen_nichtNumerischeId_wirft400()
        {
            assertThatThrownBy(() -> service.hochladen("MITGLIED", "abc", "a.pdf", "application/pdf", INHALT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("unbekannte Entity -> 404")
        void hochladen_unbekannteEntity_wirft404()
        {
            when(mitglieder.existsById(42L)).thenReturn(false);

            assertThatThrownBy(() -> service.hochladen("MITGLIED", "42", "a.pdf", "application/pdf", INHALT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("unbekannter Verein -> 404")
        void hochladen_unbekannterVerein_wirft404()
        {
            when(vereine.existsById("Fantasieverein")).thenReturn(false);

            assertThatThrownBy(() -> service.hochladen("VEREIN", "Fantasieverein", "a.pdf", "application/pdf", INHALT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("leerer Inhalt -> 400")
        void hochladen_leererInhalt_wirft400()
        {
            when(mitglieder.existsById(1L)).thenReturn(true);

            assertThatThrownBy(() -> service.hochladen("MITGLIED", "1", "a.pdf", "application/pdf", new byte[0]))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(dokumente, never()).save(any());
        }

        @Test
        @DisplayName("Datei über 10 MB -> 413")
        void hochladen_zuGross_wirft413()
        {
            when(mitglieder.existsById(1L)).thenReturn(true);
            byte[] zuGross = new byte[(int) DokumentService.MAX_DATEIGROESSE + 1];

            assertThatThrownBy(() -> service.hochladen("MITGLIED", "1", "a.pdf", "application/pdf", zuGross))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        }

        @Test
        @DisplayName("fehlender Dateiname -> 400")
        void hochladen_ohneDateiname_wirft400()
        {
            when(mitglieder.existsById(1L)).thenReturn(true);

            assertThatThrownBy(() -> service.hochladen("MITGLIED", "1", "  ", "application/pdf", INHALT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("hochladen (Multipart)")
    class HochladenMultipart {

        @Test
        @DisplayName("Pfadanteile im Dateinamen werden entfernt")
        void hochladen_multipartMitPfad_entferntVerzeichnisse()
        {
            mitgliedExistiert();
            MockMultipartFile datei = new MockMultipartFile(
                    "datei", "C:\\Users\\ole\\Desktop\\brief.pdf", MediaType.APPLICATION_PDF_VALUE, INHALT);

            service.hochladen("MITGLIED", "1", datei);

            verify(dokumente).save(gespeichert.capture());
            assertThat(gespeichert.getValue().getDateiname()).isEqualTo("brief.pdf");
        }

        @Test
        @DisplayName("Pfad-Traversal im Dateinamen wird entschärft")
        void hochladen_multipartMitTraversal_entferntPfad()
        {
            mitgliedExistiert();
            MockMultipartFile datei = new MockMultipartFile(
                    "datei", "../../etc/passwd", MediaType.TEXT_PLAIN_VALUE, INHALT);

            service.hochladen("MITGLIED", "1", datei);

            verify(dokumente).save(gespeichert.capture());
            assertThat(gespeichert.getValue().getDateiname())
                    .isEqualTo("passwd")
                    .doesNotContain("..", "/", "\\");
        }

        @Test
        @DisplayName("leere Datei -> 400")
        void hochladen_leereMultipartDatei_wirft400()
        {
            MockMultipartFile leer = new MockMultipartFile("datei", "a.pdf", "application/pdf", new byte[0]);

            assertThatThrownBy(() -> service.hochladen("MITGLIED", "1", leer))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("herunterladen und löschen")
    class HerunterladenUndLoeschen {

        @Test
        @DisplayName("unbekannte ID beim Download -> 404")
        void herunterladen_unbekannteId_wirft404()
        {
            when(dokumente.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.herunterladen(5L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("unbekannte ID beim Löschen -> 404, ohne delete-Aufruf")
        void loeschen_unbekannteId_wirft404()
        {
            when(dokumente.existsById(5L)).thenReturn(false);

            assertThatThrownBy(() -> service.loeschen(5L))
                    .isInstanceOf(ResponseStatusException.class);
            verify(dokumente, never()).deleteById(any());
        }

        @Test
        @DisplayName("vorhandenes Dokument wird gelöscht")
        void loeschen_vorhanden_loescht()
        {
            when(dokumente.existsById(5L)).thenReturn(true);

            service.loeschen(5L);

            verify(dokumente).deleteById(5L);
        }
    }
}
