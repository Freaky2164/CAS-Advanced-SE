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
 * @author Nils
 *
 * Benutzerverwaltung für Administratoren (ersetzt die fehlende Pflege aus
 * compucrash.user_def / CLoginFrame – im Altsystem gab es dafür keine UI,
 * Benutzer wurden direkt in der DB gepflegt). Anlegen, Rolle ändern,
 * Aktivieren/Deaktivieren und Passwort-Reset; harte Löschungen sind bewusst
 * nicht vorgesehen, damit spätere Änderungshistorien (Envers) auf einen
 * bestehenden Benutzer verweisen können.
 */
@Service
@Transactional
public class AppUserService {

    /**
     * @author Nils
     *
     * Öffentlich sichtbare Benutzerdaten ohne Passwort-Hash.
     */
    public record AppUserResponse(Long id, String username, AppUser.Role role, boolean enabled, OffsetDateTime createdAt) {
        static AppUserResponse of(AppUser u) {
            return new AppUserResponse(u.getId(), u.getUsername(), u.getRole(), u.isEnabled(), u.getCreatedAt());
        }
    }

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AppUserService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<AppUserResponse> alle() {
        return users.findAll(Sort.by("username")).stream().map(AppUserResponse::of).toList();
    }

    /**
     * @author Nils
     *
     * Legt einen neuen Benutzer mit BCrypt-gehashtem Passwort an.
     */
    public AppUserResponse anlegen(String username, String passwort, AppUser.Role role) {
        if (users.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Benutzername '" + username + "' existiert bereits");
        }
        AppUser u = new AppUser(username, encoder.encode(passwort), role);
        return AppUserResponse.of(users.save(u));
    }

    /**
     * @author Nils
     *
     * Ändert Rolle und Aktiv-Status. Verhindert, dass der letzte aktive Administrator
     * degradiert oder deaktiviert wird (sonst kann sich niemand mehr anmelden, der
     * weitere Admins verwalten könnte).
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
     * @author Nils
     *
     * Setzt das Passwort eines Benutzers zurück (Admin-Funktion, kein Einmal-Link-Flow nötig).
     */
    public AppUserResponse passwortZuruecksetzen(Long id, String neuesPasswort) {
        AppUser u = finden(id);
        u.setPasswordHash(encoder.encode(neuesPasswort));
        return AppUserResponse.of(u);
    }

    private AppUser finden(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Benutzer " + id + " nicht gefunden"));
    }

    private long aktiveAdmins() {
        return users.findAll().stream()
                .filter(u -> u.getRole() == AppUser.Role.ADMIN && u.isEnabled())
                .count();
    }
}
