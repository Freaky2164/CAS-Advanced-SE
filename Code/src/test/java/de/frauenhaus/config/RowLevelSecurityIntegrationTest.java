package de.frauenhaus.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integrationstest der Datenbank-Härtung aus den Flyway-Migrationen {@code V6}
 * (append-only Historie) und {@code V7} (Rolle, Rechte, Row Level Security).
 *
 * <p>Die übrigen Tests verbinden sich als Schema-Eigentümer und sind von Row
 * Level Security ausgenommen – dieser Test verbindet sich bewusst als die
 * eingeschränkte Anwendungsrolle {@code frauenhaus_backend} und weist damit
 * nach, dass</p>
 * <ul>
 *   <li>ohne gesetzten Benutzerkontext keine personenbezogenen Zeilen sichtbar sind,</li>
 *   <li>nur die Rollen {@code ADMIN} und {@code SACHBEARBEITUNG} Zugriff erhalten,</li>
 *   <li>die Audit-Historie für die Anwendungsrolle append-only ist,</li>
 *   <li>die Rolle weder Umgehungsrechte noch DDL noch Zugriff auf die
 *       Flyway-Historie besitzt.</li>
 * </ul>
 *
 * <p>Existiert die Rolle nicht – etwa auf einer nur per Flyway erzeugten
 * Datenbank, bei der weder {@code db/init/05_sicherheit.sh} lief noch
 * {@code DB_APP_PASSWORD}/{@code DB_PASSWORD} für {@code V7} gesetzt war –,
 * wird der Test übersprungen statt fehlzuschlagen.</p>
 *
 * @author Paul
 */
@SpringBootTest
class RowLevelSecurityIntegrationTest {

    private static final String APP_ROLLE = "frauenhaus_backend";
    private static final String TEST_NAME = "RLS-Testmitglied";

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String eigentuemer;

    @Value("${spring.datasource.password}")
    private String eigentuemerPasswort;

    @Value("${DB_APP_PASSWORD:frauenhaus}")
    private String appRollePasswort;

    private long mitgliedId;

    private Connection alsEigentuemer() throws SQLException
    {
        return DriverManager.getConnection(jdbcUrl, eigentuemer, eigentuemerPasswort);
    }

    private Connection alsAnwendungsrolle() throws SQLException
    {
        return DriverManager.getConnection(jdbcUrl, APP_ROLLE, appRollePasswort);
    }

    private boolean rolleVorhanden() throws SQLException
    {
        try (Connection c = alsEigentuemer();
             PreparedStatement s = c.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = ?")) {
            s.setString(1, APP_ROLLE);
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void setzeKontext(Connection c, String benutzer, String rolle) throws SQLException
    {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT set_config('app.benutzer', ?, false), set_config('app.benutzer_rolle', ?, false)")) {
            s.setString(1, benutzer);
            s.setString(2, rolle);
            s.execute();
        }
    }

    private long sichtbareTestmitglieder(Connection c) throws SQLException
    {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT count(*) FROM frauenhaus.mitglied WHERE mitglied = ?")) {
            s.setLong(1, mitgliedId);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    @BeforeEach
    void testmitgliedAnlegen() throws SQLException
    {
        assumeTrue(rolleVorhanden(),
                "Rolle " + APP_ROLLE + " fehlt – Datenbank ohne db/init/05_sicherheit.sh bzw. ohne "
                        + "DB_APP_PASSWORD für Migration V7, Test wird übersprungen");

        try (Connection c = alsEigentuemer();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO frauenhaus.mitglied (name) VALUES (?) RETURNING mitglied")) {
            s.setString(1, TEST_NAME);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                mitgliedId = rs.getLong(1);
            }
        }
    }

    @AfterEach
    void testmitgliedEntfernen() throws SQLException
    {
        if (mitgliedId == 0) {
            return;
        }
        try (Connection c = alsEigentuemer();
             PreparedStatement s = c.prepareStatement("DELETE FROM frauenhaus.mitglied WHERE mitglied = ?")) {
            s.setLong(1, mitgliedId);
            s.executeUpdate();
        }
        mitgliedId = 0;
    }

    @Nested
    @DisplayName("Lesezugriff der Anwendungsrolle")
    class Lesezugriff {

        @Test
        @DisplayName("ohne Benutzerkontext ist keine Zeile sichtbar")
        void ohneKontext_keineZeilenSichtbar() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                assertThat(sichtbareTestmitglieder(c))
                        .as("ein kontextloser Client darf keine personenbezogenen Daten sehen")
                        .isZero();
            }
        }

        @Test
        @DisplayName("mit Rolle SACHBEARBEITUNG ist die Zeile sichtbar")
        void mitSachbearbeitung_zeileSichtbar() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "ole", "SACHBEARBEITUNG");
                assertThat(sichtbareTestmitglieder(c)).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("mit Rolle ADMIN ist die Zeile sichtbar")
        void mitAdmin_zeileSichtbar() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "admin", "ADMIN");
                assertThat(sichtbareTestmitglieder(c)).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("eine unbekannte Rolle erhält keinen Zugriff")
        void mitUnbekannterRolle_keineZeilenSichtbar() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "angreifer", "USER");
                assertThat(sichtbareTestmitglieder(c))
                        .as("nur ADMIN und SACHBEARBEITUNG stehen in der Policy")
                        .isZero();
            }
        }

        @Test
        @DisplayName("app.app_user bleibt bewusst ohne RLS lesbar (Anmeldung)")
        void appUser_ohneKontextLesbar() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle();
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*) FROM app.app_user")) {
                rs.next();
                assertThat(rs.getLong(1)).isNotNegative();
            }
        }
    }

    @Nested
    @DisplayName("Schreibzugriff der Anwendungsrolle")
    class Schreibzugriff {

        @Test
        @DisplayName("ohne Benutzerkontext trifft ein UPDATE keine Zeile")
        void ohneKontext_updateTrifftNichts() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle();
                 PreparedStatement s = c.prepareStatement(
                         "UPDATE frauenhaus.mitglied SET ort = 'Hacktown' WHERE mitglied = ?")) {
                s.setLong(1, mitgliedId);
                assertThat(s.executeUpdate()).isZero();
            }
        }

        @Test
        @DisplayName("mit gültigem Kontext ist das UPDATE erlaubt")
        void mitKontext_updateErlaubt() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "ole", "SACHBEARBEITUNG");
                try (PreparedStatement s = c.prepareStatement(
                        "UPDATE frauenhaus.mitglied SET ort = 'Mannheim' WHERE mitglied = ?")) {
                    s.setLong(1, mitgliedId);
                    assertThat(s.executeUpdate()).isEqualTo(1);
                }
            }
        }
    }

    @Nested
    @DisplayName("Audit-Historie ist append-only")
    class AppendOnly {

        @Test
        @DisplayName("UPDATE auf mitglied_aud ist der Anwendungsrolle entzogen")
        void updateAufAuditTabelle_wirdVerweigert() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "angreifer", "ADMIN");
                try (Statement s = c.createStatement()) {
                    assertThatThrownBy(() -> s.executeUpdate("UPDATE frauenhaus.mitglied_aud SET name = 'gefaelscht'"))
                            .isInstanceOf(SQLException.class)
                            .hasMessageContaining("mitglied_aud");
                }
            }
        }

        @Test
        @DisplayName("DELETE auf mitglied_aud ist der Anwendungsrolle entzogen")
        void deleteAufAuditTabelle_wirdVerweigert() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "angreifer", "ADMIN");
                try (Statement s = c.createStatement()) {
                    assertThatThrownBy(() -> s.executeUpdate("DELETE FROM frauenhaus.mitglied_aud"))
                            .isInstanceOf(SQLException.class);
                }
            }
        }

        @Test
        @DisplayName("DELETE auf app.revinfo ist der Anwendungsrolle entzogen")
        void deleteAufRevinfo_wirdVerweigert() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "angreifer", "ADMIN");
                try (Statement s = c.createStatement()) {
                    assertThatThrownBy(() -> s.executeUpdate("DELETE FROM app.revinfo"))
                            .isInstanceOf(SQLException.class);
                }
            }
        }

        @Test
        @DisplayName("INSERT in die Historie bleibt möglich (Envers schreibt weiter)")
        void insertInHistorie_bleibtErlaubt() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle()) {
                setzeKontext(c, "ole", "SACHBEARBEITUNG");
                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO app.revinfo (revtstmp, username) VALUES (?, ?) RETURNING rev")) {
                    s.setLong(1, System.currentTimeMillis());
                    s.setString(2, "ole");
                    try (ResultSet rs = s.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                    }
                }
                // Der Eintrag bleibt bewusst stehen: die Anwendungsrolle darf ihn
                // nicht mehr löschen – genau das ist der Sinn von append-only.
            }
        }
    }

    @Nested
    @DisplayName("Rechte der Anwendungsrolle")
    class Rechte {

        @Test
        @DisplayName("die Rolle hat weder SUPERUSER noch BYPASSRLS")
        void rolle_hatKeineUmgehungsrechte() throws SQLException
        {
            try (Connection c = alsEigentuemer();
                 PreparedStatement s = c.prepareStatement(
                         "SELECT rolsuper, rolbypassrls, rolcreatedb, rolcreaterole FROM pg_roles WHERE rolname = ?")) {
                s.setString(1, APP_ROLLE);
                try (ResultSet rs = s.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getBoolean("rolsuper")).as("kein SUPERUSER").isFalse();
                    assertThat(rs.getBoolean("rolbypassrls")).as("kein BYPASSRLS").isFalse();
                    assertThat(rs.getBoolean("rolcreatedb")).as("kein CREATEDB").isFalse();
                    assertThat(rs.getBoolean("rolcreaterole")).as("kein CREATEROLE").isFalse();
                }
            }
        }

        @Test
        @DisplayName("DDL ist der Anwendungsrolle verwehrt")
        void rolle_darfKeinDdl() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle();
                 Statement s = c.createStatement()) {
                assertThatThrownBy(() -> s.executeUpdate("CREATE TABLE frauenhaus.hacktable (id int)"))
                        .isInstanceOf(SQLException.class);
            }
        }

        @Test
        @DisplayName("die Flyway-Historie ist der Anwendungsrolle entzogen")
        void rolle_siehtFlywayHistorieNicht() throws SQLException
        {
            try (Connection c = alsAnwendungsrolle();
                 Statement s = c.createStatement()) {
                assertThatThrownBy(() -> s.executeQuery("SELECT * FROM frauenhaus.flyway_schema_history"))
                        .as("die Migrationshistorie gehört dem Migrationsbenutzer, nicht der Anwendung")
                        .isInstanceOf(SQLException.class);
            }
        }
    }
}
