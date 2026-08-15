package de.frauenhaus.security;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Benutzerverwaltung für Administratoren: Anlegen, Rolle ändern,
 * Aktivieren/Deaktivieren und Passwort-Reset. Harte Löschungen sind nicht
 * vorgesehen, damit Änderungshistorien auf einen bestehenden Benutzer
 * verweisen können.
 *
 * @author Paul
 */
@Service
@Transactional
public class AppUserService {

    /** Mindestlänge eines Passworts in Zeichen. */
    public static final int MIN_PASSWORT_LAENGE = 10;

    /**
     * Maximallänge eines Passworts in Bytes. BCrypt verarbeitet nur die ersten
     * 72 Bytes; längere Passwörter würden stillschweigend abgeschnitten und
     * dem Benutzer fälschlich mehr Sicherheit suggerieren.
     */
    public static final int MAX_PASSWORT_BYTES = 72;

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
     * @throws ResponseStatusException mit Status 400, wenn Benutzername oder Passwort ungültig sind
     * @throws ResponseStatusException mit Status 409, wenn der Benutzername bereits vergeben ist
     */
    public AppUserResponse anlegen(String username, String passwort, AppUser.Role role) {
        String name = pruefeBenutzername(username);
        pruefePasswort(passwort);
        if (users.findByUsername(name).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Benutzername '" + name + "' existiert bereits");
        }
        AppUser u = new AppUser(name, encoder.encode(passwort), role);
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
     * @throws ResponseStatusException mit Status 400, wenn das Passwort die Regeln verletzt
     * @throws ResponseStatusException mit Status 404, wenn der Benutzer nicht existiert
     */
    public AppUserResponse passwortZuruecksetzen(Long id, String neuesPasswort) {
        pruefePasswort(neuesPasswort);
        AppUser u = finden(id);
        u.setPasswordHash(encoder.encode(neuesPasswort));
        return AppUserResponse.of(u);
    }

    /**
     * Prüft den Benutzernamen auf Inhalt und Länge und liefert ihn getrimmt
     * zurück.
     *
     * @param username der zu prüfende Benutzername
     * @return der getrimmte Benutzername
     */
    private static String pruefeBenutzername(String username) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Benutzername darf nicht leer sein");
        }
        if (name.length() > 50) {
            throw new ResponseStatusException(BAD_REQUEST, "Benutzername darf höchstens 50 Zeichen lang sein");
        }
        return name;
    }

    /**
     * Prüft das Passwort gegen die Mindest- und Höchstlänge. Bewusst keine
     * Zeichenklassen-Pflicht: Länge ist der wirksamere Faktor, erzwungene
     * Sonderzeichen führen erfahrungsgemäß zu notierten Passwörtern.
     *
     * @param passwort das zu prüfende Klartext-Passwort
     */
    private static void pruefePasswort(String passwort) {
        if (passwort == null || passwort.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Passwort darf nicht leer sein");
        }
        if (passwort.length() < MIN_PASSWORT_LAENGE) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Passwort muss mindestens " + MIN_PASSWORT_LAENGE + " Zeichen lang sein");
        }
        if (passwort.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORT_BYTES) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Passwort darf höchstens " + MAX_PASSWORT_BYTES + " Bytes lang sein");
        }
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
