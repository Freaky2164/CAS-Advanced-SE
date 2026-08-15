package de.frauenhaus.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests für das Anlegen des initialen Administrators beim Anwendungsstart.
 *
 * @author Paul
 */
@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Mock
    private AppUserRepository users;

    @Mock
    private ApplicationArguments arguments;

    @Captor
    private ArgumentCaptor<AppUser> angelegt;

    private void starte(String konfiguriertesPasswort) throws Exception
    {
        ApplicationRunner runner = new AdminBootstrap().createInitialAdmin(users, encoder, konfiguriertesPasswort);
        runner.run(arguments);
    }

    @Nested
    @DisplayName("leere Benutzertabelle")
    class LeereTabelle {

        @Test
        @DisplayName("konfiguriertes Passwort wird für den Admin verwendet")
        void createInitialAdmin_leereTabelleMitPasswort_legtAdminAn() throws Exception
        {
            when(users.count()).thenReturn(0L);

            starte("streng-geheimes-passwort");

            verify(users).save(angelegt.capture());
            AppUser admin = angelegt.getValue();
            assertThat(admin.getUsername()).isEqualTo("admin");
            assertThat(admin.getRole()).isEqualTo(AppUser.Role.ADMIN);
            assertThat(admin.isEnabled()).isTrue();
            assertThat(encoder.matches("streng-geheimes-passwort", admin.getPasswordHash()))
                    .as("Passwort muss BCrypt-gehasht gespeichert sein")
                    .isTrue();
        }

        @Test
        @DisplayName("ohne konfiguriertes Passwort wird ein Zufallspasswort erzeugt")
        void createInitialAdmin_leereTabelleOhnePasswort_erzeugtZufallspasswort() throws Exception
        {
            when(users.count()).thenReturn(0L);

            starte("");

            verify(users).save(angelegt.capture());
            AppUser admin = angelegt.getValue();
            assertThat(admin.getPasswordHash()).startsWith("$2");
            assertThat(encoder.matches("", admin.getPasswordHash()))
                    .as("das leere Passwort darf gerade NICHT gesetzt worden sein")
                    .isFalse();
        }

        @Test
        @DisplayName("null-Passwort verhält sich wie ein leeres Passwort")
        void createInitialAdmin_nullPasswort_erzeugtZufallspasswort() throws Exception
        {
            when(users.count()).thenReturn(0L);

            starte(null);

            verify(users).save(angelegt.capture());
            assertThat(angelegt.getValue().getPasswordHash()).startsWith("$2");
        }
    }

    @Nested
    @DisplayName("bereits befüllte Benutzertabelle")
    class BefuellteTabelle {

        @Test
        @DisplayName("es wird kein weiterer Admin angelegt")
        void createInitialAdmin_benutzerVorhanden_legtNichtsAn() throws Exception
        {
            when(users.count()).thenReturn(3L);

            starte("streng-geheimes-passwort");

            verify(users, never()).save(org.mockito.ArgumentMatchers.any());
        }
    }
}
