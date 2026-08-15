package de.frauenhaus.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests der Benutzerverwaltung: Anlegen, Passwortregeln, Rollenwechsel und
 * der Schutz des letzten aktiven Administrators.
 *
 * @author Paul
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppUserServiceTest {

    private static final String GUELTIGES_PASSWORT = "sicher-genug-123";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Mock
    private AppUserRepository users;

    @Captor
    private ArgumentCaptor<AppUser> gespeichert;

    private AppUserService service;

    private AppUserService service()
    {
        if (service == null)
        {
            service = new AppUserService(users, encoder);
        }
        return service;
    }

    private static AppUser benutzer(String name, AppUser.Role rolle, boolean aktiv)
    {
        AppUser u = new AppUser(name, "$2a$10$abcdefghijklmnopqrstuv", rolle);
        u.setEnabled(aktiv);
        return u;
    }

    @Nested
    @DisplayName("anlegen")
    class Anlegen {

        @Test
        @DisplayName("gültige Eingaben -> Benutzer mit BCrypt-Hash")
        void anlegen_gueltigeEingaben_speichertGehashtesPasswort()
        {
            when(users.findByUsername("neu")).thenReturn(Optional.empty());
            when(users.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));

            service().anlegen("neu", GUELTIGES_PASSWORT, AppUser.Role.SACHBEARBEITUNG);

            verify(users).save(gespeichert.capture());
            AppUser angelegt = gespeichert.getValue();
            assertThat(angelegt.getUsername()).isEqualTo("neu");
            assertThat(angelegt.getPasswordHash()).isNotEqualTo(GUELTIGES_PASSWORT);
            assertThat(encoder.matches(GUELTIGES_PASSWORT, angelegt.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("Benutzername wird getrimmt")
        void anlegen_benutzernameMitLeerzeichen_wirdGetrimmt()
        {
            when(users.findByUsername("neu")).thenReturn(Optional.empty());
            when(users.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));

            service().anlegen("  neu  ", GUELTIGES_PASSWORT, AppUser.Role.SACHBEARBEITUNG);

            verify(users).save(gespeichert.capture());
            assertThat(gespeichert.getValue().getUsername()).isEqualTo("neu");
        }

        @Test
        @DisplayName("vergebener Benutzername -> 409")
        void anlegen_vorhandenerBenutzername_wirft409()
        {
            when(users.findByUsername("admin"))
                    .thenReturn(Optional.of(benutzer("admin", AppUser.Role.ADMIN, true)));

            assertThatThrownBy(() -> service().anlegen("admin", GUELTIGES_PASSWORT, AppUser.Role.ADMIN))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @ParameterizedTest(name = "Benutzername \"{0}\" -> 400")
        @ValueSource(strings = {"", "   "})
        @DisplayName("leerer Benutzername -> 400")
        void anlegen_leererBenutzername_wirft400(String username)
        {
            assertThatThrownBy(() -> service().anlegen(username, GUELTIGES_PASSWORT, AppUser.Role.ADMIN))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(users, never()).save(any());
        }

        @Test
        @DisplayName("null als Benutzername -> 400")
        void anlegen_nullBenutzername_wirft400()
        {
            assertThatThrownBy(() -> service().anlegen(null, GUELTIGES_PASSWORT, AppUser.Role.ADMIN))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @ParameterizedTest(name = "Passwort \"{0}\" -> 400")
        @ValueSource(strings = {"", "   ", "kurz", "123456789"})
        @DisplayName("zu kurzes oder leeres Passwort -> 400")
        void anlegen_ungueltigesPasswort_wirft400(String passwort)
        {
            assertThatThrownBy(() -> service().anlegen("neu", passwort, AppUser.Role.SACHBEARBEITUNG))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(users, never()).save(any());
        }

        @Test
        @DisplayName("Passwort exakt an der Mindestlänge wird akzeptiert")
        void anlegen_passwortMitMindestlaenge_wirdAkzeptiert()
        {
            String genauLangGenug = "a".repeat(AppUserService.MIN_PASSWORT_LAENGE);
            when(users.findByUsername("neu")).thenReturn(Optional.empty());
            when(users.save(any())).thenAnswer(aufruf -> aufruf.getArgument(0));

            assertThat(service().anlegen("neu", genauLangGenug, AppUser.Role.SACHBEARBEITUNG)).isNotNull();
        }

        @Test
        @DisplayName("Passwort über der BCrypt-Grenze von 72 Bytes -> 400")
        void anlegen_zuLangesPasswort_wirft400()
        {
            String zuLang = "a".repeat(AppUserService.MAX_PASSWORT_BYTES + 1);

            assertThatThrownBy(() -> service().anlegen("neu", zuLang, AppUser.Role.SACHBEARBEITUNG))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("72");
        }

        @Test
        @DisplayName("Mehrbyte-Zeichen zählen nach Bytes, nicht nach Zeichen")
        void anlegen_mehrbytePasswortUeberGrenze_wirft400()
        {
            // 30 Zeichen, aber 90 Bytes in UTF-8
            String mehrbyte = "€".repeat(30);

            assertThatThrownBy(() -> service().anlegen("neu", mehrbyte, AppUser.Role.SACHBEARBEITUNG))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("passwortZuruecksetzen")
    class PasswortZuruecksetzen {

        @Test
        @DisplayName("gültiges Passwort -> neuer Hash")
        void passwortZuruecksetzen_gueltig_setztNeuenHash()
        {
            AppUser vorhanden = benutzer("ole", AppUser.Role.SACHBEARBEITUNG, true);
            when(users.findById(7L)).thenReturn(Optional.of(vorhanden));

            service().passwortZuruecksetzen(7L, GUELTIGES_PASSWORT);

            assertThat(encoder.matches(GUELTIGES_PASSWORT, vorhanden.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("zu kurzes Passwort -> 400, Benutzer wird nicht einmal geladen")
        void passwortZuruecksetzen_zuKurz_wirft400()
        {
            assertThatThrownBy(() -> service().passwortZuruecksetzen(7L, "kurz"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(users, never()).findById(any());
        }

        @Test
        @DisplayName("unbekannte ID -> 404")
        void passwortZuruecksetzen_unbekannteId_wirft404()
        {
            when(users.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().passwortZuruecksetzen(99L, GUELTIGES_PASSWORT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("aendern")
    class Aendern {

        @Test
        @DisplayName("letzter aktiver Admin kann nicht degradiert werden")
        void aendern_letzterAktiverAdminDegradiert_wirft409()
        {
            AppUser admin = benutzer("admin", AppUser.Role.ADMIN, true);
            when(users.findById(1L)).thenReturn(Optional.of(admin));
            when(users.findAll()).thenReturn(List.of(admin));

            assertThatThrownBy(() -> service().aendern(1L, AppUser.Role.SACHBEARBEITUNG, true))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
            assertThat(admin.getRole()).isEqualTo(AppUser.Role.ADMIN);
        }

        @Test
        @DisplayName("letzter aktiver Admin kann nicht deaktiviert werden")
        void aendern_letzterAktiverAdminDeaktiviert_wirft409()
        {
            AppUser admin = benutzer("admin", AppUser.Role.ADMIN, true);
            when(users.findById(1L)).thenReturn(Optional.of(admin));
            when(users.findAll()).thenReturn(List.of(admin));

            assertThatThrownBy(() -> service().aendern(1L, AppUser.Role.ADMIN, false))
                    .isInstanceOf(ResponseStatusException.class);
            assertThat(admin.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("bei zwei aktiven Admins ist die Degradierung erlaubt")
        void aendern_zweiAktiveAdmins_degradierungErlaubt()
        {
            AppUser admin = benutzer("admin", AppUser.Role.ADMIN, true);
            AppUser zweiter = benutzer("admin2", AppUser.Role.ADMIN, true);
            when(users.findById(1L)).thenReturn(Optional.of(admin));
            when(users.findAll()).thenReturn(List.of(admin, zweiter));

            service().aendern(1L, AppUser.Role.SACHBEARBEITUNG, true);

            assertThat(admin.getRole()).isEqualTo(AppUser.Role.SACHBEARBEITUNG);
        }

        @Test
        @DisplayName("deaktivierte Admins zählen nicht als aktiv")
        void aendern_zweiterAdminDeaktiviert_wirft409()
        {
            AppUser admin = benutzer("admin", AppUser.Role.ADMIN, true);
            AppUser inaktiv = benutzer("admin2", AppUser.Role.ADMIN, false);
            when(users.findById(1L)).thenReturn(Optional.of(admin));
            when(users.findAll()).thenReturn(List.of(admin, inaktiv));

            assertThatThrownBy(() -> service().aendern(1L, AppUser.Role.SACHBEARBEITUNG, true))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Sachbearbeitung kann jederzeit deaktiviert werden")
        void aendern_sachbearbeitung_deaktivierungErlaubt()
        {
            AppUser sachbearbeiter = benutzer("ole", AppUser.Role.SACHBEARBEITUNG, true);
            when(users.findById(2L)).thenReturn(Optional.of(sachbearbeiter));

            service().aendern(2L, AppUser.Role.SACHBEARBEITUNG, false);

            assertThat(sachbearbeiter.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("alle")
    class Alle {

        @Test
        @DisplayName("liefert keine Passwort-Hashes nach außen")
        void alle_liefertResponsesOhneHash()
        {
            when(users.findAll(any(Sort.class)))
                    .thenReturn(List.of(benutzer("admin", AppUser.Role.ADMIN, true)));

            List<AppUserService.AppUserResponse> ergebnis = service().alle();

            assertThat(ergebnis).singleElement()
                    .satisfies(u -> {
                        assertThat(u.username()).isEqualTo("admin");
                        assertThat(u.role()).isEqualTo(AppUser.Role.ADMIN);
                    });
            assertThat(ergebnis.toString()).doesNotContain("$2a$");
        }
    }
}
