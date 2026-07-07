package de.frauenhaus.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Nils
 *
 * Liefert den aktuell angemeldeten Benutzer.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    /** Benutzername und Rollen des angemeldeten Benutzers. */
    @GetMapping
    public Map<String, Object> me(Authentication auth) {
        List<String> rollen = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Map.of("username", auth.getName(), "roles", rollen);
    }
}
