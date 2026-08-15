package de.frauenhaus.service;

import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.repository.StichwortRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests der Stichwort-Pflege: Zusammenstellen behält die Ausgangsstichworte,
 * Zusammenfassen löscht sie – und zwar erst nach dem Umhängen der Mitglieder.
 *
 * @author Nils
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StichwortServiceTest {

    private static final List<String> ALTE = List.of("Newsletter", "Weihnachtsbrief");

    @Mock
    private StichwortRepository stichworte;

    @InjectMocks
    private StichwortService service;

    @Captor
    private ArgumentCaptor<Stichwort> angelegt;

    @Nested
    @DisplayName("zusammenstellen")
    class Zusammenstellen {

        @Test
        @DisplayName("legt das Zielstichwort an, wenn es fehlt")
        void zusammenstellen_neuesZiel_wirdAngelegt()
        {
            when(stichworte.findById("Alle")).thenReturn(Optional.empty());
            when(stichworte.stichworteZuordnen("Alle", ALTE)).thenReturn(7);

            int zugeordnet = service.zusammenstellen("Alle", ALTE);

            verify(stichworte).save(angelegt.capture());
            assertThat(angelegt.getValue().getName()).isEqualTo("Alle");
            assertThat(zugeordnet).isEqualTo(7);
        }

        @Test
        @DisplayName("vorhandenes Zielstichwort wird nicht erneut angelegt")
        void zusammenstellen_vorhandenesZiel_wirdNichtAngelegt()
        {
            when(stichworte.findById("Alle")).thenReturn(Optional.of(new Stichwort("Alle")));
            when(stichworte.stichworteZuordnen("Alle", ALTE)).thenReturn(3);

            service.zusammenstellen("Alle", ALTE);

            verify(stichworte, never()).save(any());
        }

        @Test
        @DisplayName("die Ausgangsstichworte bleiben erhalten")
        void zusammenstellen_loeschtNichts()
        {
            when(stichworte.findById("Alle")).thenReturn(Optional.of(new Stichwort("Alle")));

            service.zusammenstellen("Alle", ALTE);

            verify(stichworte, never()).zuordnungenLoeschen(any());
            verify(stichworte, never()).stichworteLoeschen(any());
        }
    }

    @Nested
    @DisplayName("zusammenfassen")
    class Zusammenfassen {

        @Test
        @DisplayName("ordnet zuerst um und löscht erst danach")
        void zusammenfassen_reihenfolge_umhaengenVorLoeschen()
        {
            when(stichworte.findById("Alle")).thenReturn(Optional.of(new Stichwort("Alle")));
            when(stichworte.stichworteZuordnen("Alle", ALTE)).thenReturn(5);

            int zugeordnet = service.zusammenfassen("Alle", ALTE);

            InOrder reihenfolge = inOrder(stichworte);
            reihenfolge.verify(stichworte).stichworteZuordnen("Alle", ALTE);
            reihenfolge.verify(stichworte).zuordnungenLoeschen(ALTE);
            reihenfolge.verify(stichworte).stichworteLoeschen(ALTE);
            assertThat(zugeordnet).isEqualTo(5);
        }

        @Test
        @DisplayName("legt ein fehlendes Zielstichwort vorher an")
        void zusammenfassen_neuesZiel_wirdAngelegt()
        {
            when(stichworte.findById("Alle")).thenReturn(Optional.empty());

            service.zusammenfassen("Alle", ALTE);

            verify(stichworte).save(any(Stichwort.class));
        }

        @Test
        @DisplayName("leere Ausgangsliste ordnet nichts zu")
        void zusammenfassen_leereListe_ordnetNichtsZu()
        {
            when(stichworte.findById("Alle")).thenReturn(Optional.of(new Stichwort("Alle")));
            when(stichworte.stichworteZuordnen("Alle", List.of())).thenReturn(0);

            assertThat(service.zusammenfassen("Alle", List.of())).isZero();
        }
    }
}
