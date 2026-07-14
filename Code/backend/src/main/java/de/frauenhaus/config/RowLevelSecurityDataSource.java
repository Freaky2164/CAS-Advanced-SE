package de.frauenhaus.config;

import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Nils
 *
 * Reicht den angemeldeten Anwendungsbenutzer bei jeder Connection-Ausleihe als
 * Postgres-Session-Variablen ({@code app.benutzer}, {@code app.benutzer_rolle})
 * an die Datenbank durch. Die Row-Level-Security-Policies
 * (db/init/05_sicherheit.sh) geben Zeilen mit personenbezogenen Daten nur frei,
 * wenn dieser Kontext gesetzt ist – eine DB-Verbindung ohne authentifizierten
 * Benutzer (z.B. direktes psql mit den Backend-Zugangsdaten) sieht keine Daten.
 *
 * <p>Der Kontext wird bei JEDER Ausleihe gesetzt (leer, wenn kein Benutzer
 * angemeldet ist). Damit kann eine an den Pool zurückgegebene Verbindung den
 * Kontext des vorherigen Benutzers nie in die nächste Ausleihe verschleppen.</p>
 */
public class RowLevelSecurityDataSource extends DelegatingDataSource {

    /**
     * @author Nils
     *
     * Umhüllt die eigentliche (gepoolte) DataSource.
     */
    public RowLevelSecurityDataSource(DataSource zielDataSource) {
        super(zielDataSource);
    }

    /**
     * @author Nils
     *
     * Liefert eine Verbindung mit gesetztem Benutzerkontext.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return mitBenutzerkontext(super.getConnection());
    }

    /**
     * @author Nils
     *
     * Liefert eine Verbindung mit gesetztem Benutzerkontext (Variante mit Zugangsdaten).
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return mitBenutzerkontext(super.getConnection(username, password));
    }

    /**
     * @author Nils
     *
     * Setzt Benutzername und Rolle des aktuell angemeldeten Benutzers als
     * Session-Variablen; ohne Anmeldung werden beide geleert. set_config mit
     * Parametern statt String-Konkatenation – der Benutzername ist Fremdeingabe.
     */
    private static Connection mitBenutzerkontext(Connection connection) throws SQLException {
        String benutzer = "";
        String rolle = "";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            benutzer = auth.getName();
            rolle = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring("ROLE_".length()))
                    .findFirst()
                    .orElse("");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT set_config('app.benutzer', ?, false), set_config('app.benutzer_rolle', ?, false)")) {
            statement.setString(1, benutzer);
            statement.setString(2, rolle);
            statement.execute();
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
