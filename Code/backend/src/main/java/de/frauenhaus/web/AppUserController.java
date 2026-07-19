package de.frauenhaus.web;

import de.frauenhaus.security.AppUser;
import de.frauenhaus.security.AppUserService;
import de.frauenhaus.security.AppUserService.AppUserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * REST-Endpunkte der Benutzerverwaltung. Nur für die Rolle ADMIN erreichbar
 * (Präfix {@code /api/admin/**}).
 *
 * @author Robin
 */
@RestController
@RequestMapping("/api/admin/users")
public class AppUserController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record CreateRequest(@NotBlank String username, @NotBlank @Size(min = 8) String passwort, @NotNull AppUser.Role role) { }

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record UpdateRequest(@NotNull AppUser.Role role, boolean enabled) { }

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record PasswordResetRequest(@NotBlank @Size(min = 8) String neuesPasswort) { }

    private final AppUserService appUserService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public List<AppUserResponse> alle() {
        return appUserService.alle();
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<AppUserResponse> anlegen(@Valid @RequestBody CreateRequest request) {
        AppUserResponse angelegt = appUserService.anlegen(request.username(), request.passwort(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Rolle und Aktiv-Status ändern; ein Benutzer darf sich nicht selbst deaktivieren oder degradieren.
     */
    @PutMapping("/{id}")
    public AppUserResponse aendern(@PathVariable Long id, @Valid @RequestBody UpdateRequest request, Authentication auth) {
        pruefeNichtSelbst(id, auth, "Sie können sich nicht selbst deaktivieren oder degradieren");
        return appUserService.aendern(id, request.role(), request.enabled());
    }

    /**
     * Setzt das Passwort eines Benutzers zurück.
     */
    @PutMapping("/{id}/passwort")
    public AppUserResponse passwortZuruecksetzen(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        return appUserService.passwortZuruecksetzen(id, request.neuesPasswort());
    }

    /**
     * Verhindert, dass ein Benutzer die Aktion auf den eigenen Account anwendet.
     */
    private void pruefeNichtSelbst(Long id, Authentication auth, String meldung) {
        boolean istEigenerBenutzer = appUserService.alle().stream()
                .anyMatch(u -> u.id().equals(id) && u.username().equals(auth.getName()));
        if (istEigenerBenutzer) {
            throw new ResponseStatusException(CONFLICT, meldung);
        }
    }
}
