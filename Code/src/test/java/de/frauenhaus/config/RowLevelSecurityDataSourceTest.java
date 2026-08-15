package de.frauenhaus.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests des Benutzerkontexts, den die {@link RowLevelSecurityDataSource} bei
 * jeder Connection-Ausleihe an Postgres durchreicht. Der Kontext steuert die
 * Row-Level-Security-Policies und ist damit sicherheitsrelevant.
 *
 * @author Paul
 */
@ExtendWith(MockitoExtension.class)
class RowLevelSecurityDataSourceTest {

    private static final String SET_CONFIG =
            "SELECT set_config('app.benutzer', ?, false), set_config('app.benutzer_rolle', ?, false)";

    @Mock
    private DataSource ziel;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement statement;

    @AfterEach
    void kontextAufraeumen()
    {
        SecurityContextHolder.clearContext();
    }

    private void angemeldetAls(String benutzer, String... autoritaeten)
    {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                benutzer, "n/a", List.of(autoritaeten).stream().map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    @DisplayName("angemeldeter Benutzer -> Name und Rolle ohne ROLE_-Präfix")
    void getConnection_angemeldet_setztBenutzerkontext() throws SQLException
    {
        when(ziel.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);
        angemeldetAls("ole", "ROLE_SACHBEARBEITUNG");

        Connection ergebnis = new RowLevelSecurityDataSource(ziel).getConnection();

        assertThat(ergebnis).isSameAs(connection);
        verify(statement).setString(1, "ole");
        verify(statement).setString(2, "SACHBEARBEITUNG");
        verify(statement).execute();
    }

    @Test
    @DisplayName("Administrator -> Rolle ADMIN")
    void getConnection_admin_setztRolleAdmin() throws SQLException
    {
        when(ziel.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);
        angemeldetAls("chef", "ROLE_ADMIN");

        new RowLevelSecurityDataSource(ziel).getConnection();

        verify(statement).setString(2, "ADMIN");
    }

    @Test
    @DisplayName("nicht angemeldet -> leerer Kontext (RLS blockt dann alles)")
    void getConnection_nichtAngemeldet_setztLeerenKontext() throws SQLException
    {
        when(ziel.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);

        new RowLevelSecurityDataSource(ziel).getConnection();

        verify(statement).setString(1, "");
        verify(statement).setString(2, "");
    }

    @Test
    @DisplayName("anonymer Benutzer zählt nicht als angemeldet")
    void getConnection_anonym_setztLeerenKontext() throws SQLException
    {
        when(ziel.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "schluessel", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        new RowLevelSecurityDataSource(ziel).getConnection();

        verify(statement).setString(1, "");
        verify(statement).setString(2, "");
    }

    @Test
    @DisplayName("Autorität ohne ROLE_-Präfix ergibt leere Rolle statt Fehlinterpretation")
    void getConnection_autoritaetOhnePraefix_setztLeereRolle() throws SQLException
    {
        when(ziel.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);
        angemeldetAls("ole", "SCOPE_read");

        new RowLevelSecurityDataSource(ziel).getConnection();

        verify(statement).setString(2, "");
    }

    @Test
    @DisplayName("scheitert das Setzen des Kontexts, wird die Verbindung geschlossen")
    void getConnection_fehlerBeimSetzen_schliesstVerbindung() throws SQLException
    {
        when(ziel.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);
        doThrow(new SQLException("kaputt")).when(statement).execute();

        RowLevelSecurityDataSource dataSource = new RowLevelSecurityDataSource(ziel);

        assertThatThrownBy(dataSource::getConnection).isInstanceOf(SQLException.class);
        verify(connection).close();
    }

    @Test
    @DisplayName("Variante mit Zugangsdaten setzt den Kontext ebenfalls")
    void getConnectionMitZugangsdaten_setztBenutzerkontext() throws SQLException
    {
        when(ziel.getConnection("u", "p")).thenReturn(connection);
        when(connection.prepareStatement(SET_CONFIG)).thenReturn(statement);
        angemeldetAls("ole", "ROLE_ADMIN");

        new RowLevelSecurityDataSource(ziel).getConnection("u", "p");

        verify(statement).setString(1, "ole");
        verify(statement).setString(2, "ADMIN");
    }
}
