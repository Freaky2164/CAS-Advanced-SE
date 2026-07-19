package de.frauenhaus.web;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.StichwortRepository;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für den SMTP-basierten Verteiler-Versand.
 *
 * @author Robin
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "management.health.mail.enabled=false")
class VerteilerVersandIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MitgliedRepository mitglieder;
    @Autowired
    private StichwortRepository stichworte;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUpMailSender() {
        reset(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @WithMockUser(username = "sachbearbeitung")
    void versendetEineSammelmailMitBccUndBetreff() throws Exception {
        String suffix = suffix();
        Stichwort stichwort = stichworte.saveAndFlush(new Stichwort("Newsletter" + suffix));
        mitglieder.saveAndFlush(mitgliedMitStichwort("Anna", "Alpha" + suffix, "anna-" + suffix + "@test.invalid", stichwort));
        mitglieder.saveAndFlush(mitgliedMitStichwort("Berta", "Beta" + suffix, "berta-" + suffix + "@test.invalid", stichwort));
        mitglieder.saveAndFlush(mitgliedMitStichwort("Clara", "OhneMail" + suffix, null, stichwort));

        mockMvc.perform(post("/api/reports/verteiler/versenden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stichworte": ["Newsletter%s"],
                                  "betreff": "Sommerbrief",
                                  "text": "Liebe Unterstuetzerinnen und Unterstuetzer"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empfaengerAnzahl").value(2));

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage nachricht = messageCaptor.getValue();
        assertThat(nachricht.getSubject()).isEqualTo("Sommerbrief");
        assertThat(nachricht.getContent().toString()).contains("Liebe Unterstuetzerinnen und Unterstuetzer");
        assertThat(adressen(nachricht.getRecipients(Message.RecipientType.TO))).containsExactly("no-reply@localhost");
        assertThat(adressen(nachricht.getRecipients(Message.RecipientType.BCC)))
                .containsExactly("anna-" + suffix + "@test.invalid", "berta-" + suffix + "@test.invalid");
    }

    @Test
    @WithMockUser(username = "sachbearbeitung")
    void meldetKeineEmpfaengerAlsBadRequest() throws Exception {
        mockMvc.perform(post("/api/reports/verteiler/versenden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stichworte": ["Unbekannt"],
                                  "betreff": "Leer",
                                  "text": "Niemand da"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Keine E-Mail-Empfänger zu den gewählten Stichworten gefunden"));
    }

    @Test
    @WithMockUser(username = "sachbearbeitung")
    void uebersetztMailFehlerInSaubereApiAntwort() throws Exception {
        String suffix = suffix();
        Stichwort stichwort = stichworte.saveAndFlush(new Stichwort("Verteiler" + suffix));
        mitglieder.saveAndFlush(mitgliedMitStichwort("Dora", "Delta" + suffix, "dora-" + suffix + "@test.invalid", stichwort));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(post("/api/reports/verteiler/versenden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stichworte": ["Verteiler%s"],
                                  "betreff": "Fehlerfall",
                                  "text": "Bitte ignorieren"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("E-Mail konnte nicht versendet werden: SMTP down"));
    }

    /**
     * Erzeugt ein Mitglied mit dem angegebenen Stichwort.
     */
    private Mitglied mitgliedMitStichwort(String vorname, String name, String email, Stichwort stichwort) {
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname(vorname);
        mitglied.setName(name);
        mitglied.setEmail(email);
        mitglied.getStichworte().add(stichwort);
        return mitglied;
    }

    /**
     * Extrahiert die Adress-Strings aus den übergebenen Mail-Adressen.
     */
    private static java.util.List<String> adressen(Address[] adressen) {
        return java.util.Arrays.stream(adressen)
                .map(InternetAddress.class::cast)
                .map(InternetAddress::getAddress)
                .toList();
    }

    /**
     * Erzeugt ein zufälliges Suffix zur Kollisionsvermeidung der Testdaten.
     */
    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
