package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import de.frauenhaus.repository.SpendenartRepository;
import de.frauenhaus.repository.VereinRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests der Spendenpflege: Suchbegriff-Normalisierung, Auflösung der
 * Fremdschlüssel und Fehlerpfade beim Ändern und Löschen.
 *
 * @author Nils
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpendeServiceTest {

    private static final Pageable SEITE = PageRequest.of(0, 20);
    private static final LocalDate DATUM = LocalDate.of(2025, 3, 15);
    private static final BigDecimal BETRAG = new BigDecimal("50.00");

    @Mock
    private SpendeRepository spenden;
    @Mock
    private MitgliedRepository mitglieder;
    @Mock
    private SpendenartRepository spendenarten;
    @Mock
    private VereinRepository vereine;
    @Mock
    private DokumentService dokumente;

    @InjectMocks
    private SpendeService service;

    private static Mitglied mitglied()
    {
        Mitglied m = new Mitglied();
        m.setVorname("Sabine");
        m.setName("Bergmann");
        return m;
    }

    private void referenzenAufloesbar()
    {
        when(mitglieder.findById(1L)).thenReturn(Optional.of(mitglied()));
        when(spendenarten.findById("Jahresbeitrag"))
                .thenReturn(Optional.of(new Spendenart("Jahresbeitrag", "Mitgliedsbeitrag")));
        when(vereine.findById("Frauenhaus")).thenReturn(Optional.of(new Verein("Frauenhaus", "Frauenhaus")));
        when(spenden.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));
    }

    @Nested
    @DisplayName("alle")
    class Alle {

        @Test
        @DisplayName("ohne Suchbegriff wird nicht gefiltert")
        void alle_ohneSuche_nutztFindAll()
        {
            when(spenden.findAll(SEITE)).thenReturn(Page.empty(SEITE));

            service.alle(SEITE, null);

            verify(spenden).findAll(SEITE);
            verify(spenden, never()).suchen(any(), any());
        }

        @Test
        @DisplayName("leerer Suchbegriff wird wie kein Suchbegriff behandelt")
        void alle_leereSuche_nutztFindAll()
        {
            when(spenden.findAll(SEITE)).thenReturn(Page.empty(SEITE));

            service.alle(SEITE, "   ");

            verify(spenden).findAll(SEITE);
        }

        @Test
        @DisplayName("Suchbegriff wird getrimmt weitergereicht")
        void alle_suchbegriff_wirdGetrimmt()
        {
            when(spenden.suchen(eq("Bergmann"), eq(SEITE))).thenReturn(Page.empty(SEITE));

            service.alle(SEITE, "  Bergmann  ");

            verify(spenden).suchen("Bergmann", SEITE);
        }

        @Test
        @DisplayName("Mitgliedsname wird für die Anzeige zusammengesetzt")
        void alle_liefertZusammengesetztenNamen()
        {
            Spende s = new Spende();
            s.setMitglied(mitglied());
            s.setSpendenart(new Spendenart("Jahresbeitrag", "Mitgliedsbeitrag"));
            s.setVerein(new Verein("Frauenhaus", "Frauenhaus"));
            s.setDatum(DATUM);
            s.setBetrag(BETRAG);
            when(spenden.findAll(SEITE)).thenReturn(new PageImpl<>(List.of(s), SEITE, 1));

            assertThat(service.alle(SEITE, null).getContent())
                    .singleElement()
                    .satisfies(r -> assertThat(r.mitgliedName()).isEqualTo("Sabine Bergmann"));
        }
    }

    @Nested
    @DisplayName("anlegen und ändern")
    class AnlegenUndAendern {

        @Test
        @DisplayName("gültige Eingaben werden übernommen")
        void anlegen_gueltig_speichertSpende()
        {
            referenzenAufloesbar();

            SpendeService.SpendeResponse antwort =
                    service.anlegen(1L, "Jahresbeitrag", "Frauenhaus", DATUM, BETRAG, "Danke");

            assertThat(antwort.mitgliedName()).isEqualTo("Sabine Bergmann");
            assertThat(antwort.spendenart()).isEqualTo("Jahresbeitrag");
            assertThat(antwort.verein()).isEqualTo("Frauenhaus");
            assertThat(antwort.betrag()).isEqualByComparingTo(BETRAG);
            assertThat(antwort.bemerkung()).isEqualTo("Danke");
        }

        @Test
        @DisplayName("unbekanntes Mitglied -> 400")
        void anlegen_unbekanntesMitglied_wirft400()
        {
            when(mitglieder.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.anlegen(99L, "Jahresbeitrag", "Frauenhaus", DATUM, BETRAG, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(spenden, never()).save(any());
        }

        @Test
        @DisplayName("unbekannte Spendenart -> 400")
        void anlegen_unbekannteSpendenart_wirft400()
        {
            when(mitglieder.findById(1L)).thenReturn(Optional.of(mitglied()));
            when(spendenarten.findById("Fantasie")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.anlegen(1L, "Fantasie", "Frauenhaus", DATUM, BETRAG, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Fantasie");
        }

        @Test
        @DisplayName("unbekannter Verein -> 400")
        void anlegen_unbekannterVerein_wirft400()
        {
            when(mitglieder.findById(1L)).thenReturn(Optional.of(mitglied()));
            when(spendenarten.findById("Jahresbeitrag"))
                    .thenReturn(Optional.of(new Spendenart("Jahresbeitrag", "Mitgliedsbeitrag")));
            when(vereine.findById("Fantasieverein")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.anlegen(1L, "Jahresbeitrag", "Fantasieverein", DATUM, BETRAG, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Fantasieverein");
        }

        @Test
        @DisplayName("ändern einer unbekannten Spende -> 404")
        void aendern_unbekannteId_wirft404()
        {
            when(spenden.findById(77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.aendern(77L, 1L, "Jahresbeitrag", "Frauenhaus", DATUM, BETRAG, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("löschen")
    class Loeschen {

        @Test
        @DisplayName("löscht zuerst die Anhänge, dann die Spende")
        void loeschen_vorhanden_loeschtAuchDokumente()
        {
            when(spenden.existsById(3L)).thenReturn(true);

            service.loeschen(3L);

            verify(dokumente).loescheFuerEntity(Dokument.EntityTyp.SPENDE, "3");
            verify(spenden).deleteById(3L);
        }

        @Test
        @DisplayName("unbekannte Spende -> 404, ohne Löschversuch")
        void loeschen_unbekannteId_wirft404()
        {
            when(spenden.existsById(3L)).thenReturn(false);

            assertThatThrownBy(() -> service.loeschen(3L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
            verify(spenden, never()).deleteById(any());
            verify(dokumente, never()).loescheFuerEntity(any(), any());
        }
    }
}
