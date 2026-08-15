package de.frauenhaus.service;

import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.SpendeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests für die Fehlerpfade der Dokumenterzeugung: unbekannte Datensätze
 * müssen als HTTP 404 gemeldet werden, ein fehlendes Vorlagen-Verzeichnis
 * mit einer verständlichen Meldung.
 *
 * @author Robin
 */
@ExtendWith(MockitoExtension.class)
class DocumentCreationServiceTest {

    private static final String VORLAGEN_PFAD = "vorlagen";

    @Mock
    private BussgeldRepository bussgelder;

    @Mock
    private SpendeRepository spenden;

    private DocumentCreationService service()
    {
        return new DocumentCreationService(bussgelder, spenden, VORLAGEN_PFAD);
    }

    @Nested
    @DisplayName("bussgeldBestaetigung")
    class BussgeldBestaetigung {

        @Test
        @DisplayName("unbekannte ID -> 404 statt 500")
        void bussgeldBestaetigung_unbekannteId_wirft404()
        {
            when(bussgelder.findById(4711L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().bussgeldBestaetigung(4711L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("4711");
        }
    }

    @Nested
    @DisplayName("spendenBescheinigung")
    class SpendenBescheinigung {

        @Test
        @DisplayName("unbekannte ID -> 404 statt 500")
        void spendenBescheinigung_unbekannteId_wirft404()
        {
            when(spenden.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().spendenBescheinigung(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND))
                    .hasMessageContaining("99");
        }
    }
}
