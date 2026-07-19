package de.frauenhaus.security;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Benutzerverwaltung für Administratoren: Anlegen, Rolle ändern,
 * Aktivieren/Deaktivieren und Passwort-Reset. Harte Löschungen sind nicht
 * vorgesehen, damit Änderungshistorien auf einen bestehenden Benutzer
 * verweisen können.
 *
 * @author Nils
 */
@Service
@Transactional
public class AppUserService {

    /**
     * Öffentlich sichtbare Benutzerdaten ohne Passwort-Hash.
     */
    public record AppUserResponse(Long id, String username, AppUser.Role role, boolean enabled, OffsetDateTime createdAt) {
        /** Bildet einen {@link AppUser} auf die Antwortdarstellung ab. */
        static AppUserResponse of(AppUser u) {
            return new AppUserResponse(u.getId(), u.getUsername(), u.getRole(), u.isEnabled(), u.getCreatedAt());
        }
    }

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    /**
     * Erzeugt den Service mit Repository und Passwort-Encoder.
     *
     * @param users das Benutzer-Repository
     * @param encoder der Passwort-Encoder
     */
    public AppUserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /**
     * Liefert alle Benutzer sortiert nach Benutzername.
     *
     * @return die Benutzer ohne Passwort-Hashes
     */
    @Transactional(readOnly = true)
    public List<AppUserResponse> alle() {
        return users.findAll(Sort.by("username")).stream().map(AppUserResponse::of).toList();
    }

    /**
     * Legt einen neuen Benutzer mit BCrypt-gehashtem Passwort an.
     *
     * @param username der eindeutige Benutzername
     * @param passwort das Passwort im Klartext
     * @param role die Rolle des Benutzers
     * @return der angelegte Benutzer
     */
    public AppUserResponse anlegen(String username, String passwort, AppUser.Role role) {
        if (users.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Benutzername '" + username + "' existiert bereits");
        }
        AppUser u = new AppUser(username, encoder.encode(passwort), role);
        return AppUserResponse.of(users.save(u));
    }

    /**
     * Ändert Rolle und Aktiv-Status eines Benutzers. Verhindert, dass der
     * letzte aktive Administrator degradiert oder deaktiviert wird.
     *
     * @param id die ID des Benutzers
     * @param role die neue Rolle
     * @param enabled der neue Aktiv-Status
     * @return der geänderte Benutzer
     */
    public AppUserResponse aendern(Long id, AppUser.Role role, boolean enabled) {
        AppUser u = finden(id);
        boolean warLetzterAktiverAdmin = u.getRole() == AppUser.Role.ADMIN && u.isEnabled()
                && (role != AppUser.Role.ADMIN || !enabled)
                && aktiveAdmins() <= 1;
        if (warLetzterAktiverAdmin) {
            throw new ResponseStatusException(CONFLICT, "Der letzte aktive Administrator kann nicht degradiert oder deaktiviert werden");
        }
        u.setRole(role);
        u.setEnabled(enabled);
        return AppUserResponse.of(u);
    }

    /**
     * Setzt das Passwort eines Benutzers zurück.
     *
     * @param id die ID des Benutzers
     * @param neuesPasswort das neue Passwort im Klartext
     * @return der geänderte Benutzer
     */
    public AppUserResponse passwortZuruecksetzen(Long id, String neuesPasswort) {
        AppUser u = finden(id);
        u.setPasswordHash(encoder.encode(neuesPasswort));
        return AppUserResponse.of(u);
    }

    /**
     * Lädt einen Benutzer oder wirft 404, wenn er nicht existiert.
     */
    private AppUser finden(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Benutzer " + id + " nicht gefunden"));
    }

    /**
     * Zählt die aktiven Administratoren.
     */
    private long aktiveAdmins() {
        return users.findAll().stream()
                .filter(u -> u.getRole() == AppUser.Role.ADMIN && u.isEnabled())
                .count();
    }
}
