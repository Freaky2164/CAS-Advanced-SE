package de.frauenhaus.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests der Zugriffsregeln aus {@code SecurityConfig}: die REST-API ist
 * zustandslos per HTTP Basic geschützt, {@code /api/admin/**} zusätzlich auf
 * die Rolle ADMIN beschränkt, der Health-Endpunkt bleibt offen.
 *
 * @author Paul
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiSicherheitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("ohne Anmeldung")
    class OhneAnmeldung {

        @Test
        @DisplayName("Fachdaten sind gesperrt")
        @WithAnonymousUser
        void mitglieder_ohneAnmeldung_wird401() throws Exception
        {
            mockMvc.perform(get("/api/mitglieder")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Benutzerverwaltung ist gesperrt")
        @WithAnonymousUser
        void adminEndpunkt_ohneAnmeldung_wird401() throws Exception
        {
            mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Reports sind gesperrt")
        @WithAnonymousUser
        void reports_ohneAnmeldung_wird401() throws Exception
        {
            mockMvc.perform(get("/api/reports/spenden-uebersicht").param("jahr", "2025"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("der Health-Endpunkt bleibt erreichbar")
        @WithAnonymousUser
        void health_ohneAnmeldung_istErreichbar() throws Exception
        {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("als Sachbearbeitung")
    class AlsSachbearbeitung {

        @Test
        @DisplayName("Fachdaten sind lesbar")
        @WithMockUser(username = "ole", roles = "SACHBEARBEITUNG")
        void mitglieder_alsSachbearbeitung_istErlaubt() throws Exception
        {
            mockMvc.perform(get("/api/mitglieder")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("die Benutzerverwaltung bleibt verwehrt")
        @WithMockUser(username = "ole", roles = "SACHBEARBEITUNG")
        void adminEndpunkt_alsSachbearbeitung_wird403() throws Exception
        {
            mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("das Anlegen von Benutzern bleibt verwehrt")
        @WithMockUser(username = "ole", roles = "SACHBEARBEITUNG")
        void benutzerAnlegen_alsSachbearbeitung_wird403() throws Exception
        {
            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"neu","passwort":"sicher-genug-123","role":"ADMIN"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("/api/me liefert Name und Rolle")
        @WithMockUser(username = "ole", roles = "SACHBEARBEITUNG")
        void me_liefertBenutzerUndRolle() throws Exception
        {
            mockMvc.perform(get("/api/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("ole"))
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_SACHBEARBEITUNG"));
        }
    }

    @Nested
    @DisplayName("als Administrator")
    class AlsAdministrator {

        @Test
        @DisplayName("die Benutzerverwaltung ist erreichbar")
        @WithMockUser(username = "chef", roles = "ADMIN")
        void adminEndpunkt_alsAdmin_istErlaubt() throws Exception
        {
            mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("ein zu kurzes Passwort wird schon von der Validierung abgelehnt")
        @WithMockUser(username = "chef", roles = "ADMIN")
        void benutzerAnlegen_zuKurzesPasswort_wird400() throws Exception
        {
            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"neu","passwort":"kurz","role":"SACHBEARBEITUNG"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ein leerer Benutzername wird abgelehnt")
        @WithMockUser(username = "chef", roles = "ADMIN")
        void benutzerAnlegen_leererBenutzername_wird400() throws Exception
        {
            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"","passwort":"sicher-genug-123","role":"SACHBEARBEITUNG"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }
}
