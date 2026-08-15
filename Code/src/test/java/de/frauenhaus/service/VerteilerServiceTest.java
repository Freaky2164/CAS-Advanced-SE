package de.frauenhaus.service;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.repository.MitgliedRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests des Verteilers: BCC-Versand, Fehlerpfade ohne Empfänger bzw. ohne
 * konfigurierten Absender und der Aufbau des Serienbriefs.
 *
 * @author Robin
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerteilerServiceTest {

    private static final List<String> STICHWORTE = List.of("Newsletter");
    private static final String ABSENDER = "no-reply@frauenhaus.example";

    @Mock
    private MitgliedRepository mitglieder;

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<MimeMessage> versendet;

    private VerteilerService service(String absender)
    {
        return new VerteilerService(mitglieder, new WordTemplateService(), mailSender, absender);
    }

    private static Mitglied mitglied(String vorname, String name, String email)
    {
        Mitglied m = new Mitglied();
        m.setVorname(vorname);
        m.setName(name);
        m.setEmail(email);
        m.setStrasse("Ahornweg 4");
        m.setPlz("70173");
        m.setOrt("Stuttgart");
        m.setBriefanrede("Liebe Frau " + name);
        return m;
    }

    private void mailSenderBereit()
    {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Nested
    @DisplayName("versenden")
    class Versenden {

        @Test
        @DisplayName("Empfänger stehen im BCC, nicht im To")
        void versenden_mehrereEmpfaenger_nutztBcc() throws Exception
        {
            mailSenderBereit();
            when(mitglieder.findVerteilerEmails(STICHWORTE))
                    .thenReturn(List.of("a@example.org", "b@example.org"));

            VerteilerService.VersandErgebnis ergebnis =
                    service(ABSENDER).versenden(STICHWORTE, "Betreff", "Text");

            assertThat(ergebnis.empfaengerAnzahl()).isEqualTo(2);
            verify(mailSender).send(versendet.capture());
            MimeMessage nachricht = versendet.getValue();
            assertThat(nachricht.getRecipients(MimeMessage.RecipientType.BCC))
                    .extracting(Object::toString)
                    .containsExactlyInAnyOrder("a@example.org", "b@example.org");
            assertThat(nachricht.getRecipients(MimeMessage.RecipientType.TO))
                    .extracting(Object::toString)
                    .containsExactly(ABSENDER);
            assertThat(nachricht.getSubject()).isEqualTo("Betreff");
        }

        @Test
        @DisplayName("keine Empfänger -> 400, es wird nichts versendet")
        void versenden_ohneEmpfaenger_wirft400()
        {
            when(mitglieder.findVerteilerEmails(STICHWORTE)).thenReturn(List.of());

            assertThatThrownBy(() -> service(ABSENDER).versenden(STICHWORTE, "Betreff", "Text"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("fehlender Absender -> 500")
        void versenden_ohneAbsender_wirft500()
        {
            when(mitglieder.findVerteilerEmails(STICHWORTE)).thenReturn(List.of("a@example.org"));

            assertThatThrownBy(() -> service("  ").versenden(STICHWORTE, "Betreff", "Text"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("SMTP-Fehler wird als 500 mit sprechender Ursache gemeldet")
        void versenden_smtpFehler_wirft500MitUrsache()
        {
            mailSenderBereit();
            when(mitglieder.findVerteilerEmails(STICHWORTE)).thenReturn(List.of("a@example.org"));
            doThrow(new MailSendException("Verbindung abgelehnt")).when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> service(ABSENDER).versenden(STICHWORTE, "Betreff", "Text"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Verbindung abgelehnt");
        }
    }

    @Nested
    @DisplayName("adressen")
    class Adressen {

        @Test
        @DisplayName("liefert eine nicht-leere xlsx-Datei")
        void adressen_liefertXlsx()
        {
            when(mitglieder.findVerteiler(STICHWORTE))
                    .thenReturn(List.of(mitglied("Sabine", "Bergmann", "s@example.org")));

            byte[] xlsx = service(ABSENDER).adressen(STICHWORTE);

            assertThat(xlsx).isNotEmpty();
            assertThat(new String(xlsx, 0, 2)).as("xlsx ist ein ZIP-Container").isEqualTo("PK");
        }

        @Test
        @DisplayName("leerer Verteiler liefert trotzdem eine Datei mit Kopfzeile")
        void adressen_ohneEmpfaenger_liefertDatei()
        {
            when(mitglieder.findVerteiler(STICHWORTE)).thenReturn(List.of());

            assertThat(service(ABSENDER).adressen(STICHWORTE)).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("serienbrief")
    class Serienbrief {

        @Test
        @DisplayName("liefert eine nicht-leere docx-Datei")
        void serienbrief_liefertDocx()
        {
            when(mitglieder.findVerteiler(STICHWORTE))
                    .thenReturn(List.of(mitglied("Sabine", "Bergmann", "s@example.org"),
                            mitglied("Petra", "König", "p@example.org")));

            byte[] docx = service(ABSENDER).serienbrief(STICHWORTE, "Frauenhaus", "Guten Tag");

            assertThat(docx).isNotEmpty();
            assertThat(new String(docx, 0, 2)).isEqualTo("PK");
        }

        @Test
        @DisplayName("funktioniert auch ohne Brieftext")
        void serienbrief_ohneText_liefertDocx()
        {
            when(mitglieder.findVerteiler(STICHWORTE))
                    .thenReturn(List.of(mitglied("Sabine", "Bergmann", "s@example.org")));

            assertThat(service(ABSENDER).serienbrief(STICHWORTE, "Förderverein", null)).isNotEmpty();
        }

        @Test
        @DisplayName("leerer Verteiler liefert ein leeres, aber gültiges Dokument")
        void serienbrief_ohneEmpfaenger_liefertDocx()
        {
            when(mitglieder.findVerteiler(STICHWORTE)).thenReturn(List.of());

            assertThat(service(ABSENDER).serienbrief(STICHWORTE, "Frauenhaus", "Text")).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("emails")
    class Emails {

        @Test
        @DisplayName("reicht die Repository-Antwort unverändert durch")
        void emails_liefertRepositoryErgebnis()
        {
            when(mitglieder.findVerteilerEmails(STICHWORTE)).thenReturn(List.of("a@example.org"));

            assertThat(service(ABSENDER).emails(STICHWORTE)).containsExactly("a@example.org");
        }
    }
}
