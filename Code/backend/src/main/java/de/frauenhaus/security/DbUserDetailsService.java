package de.frauenhaus.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Lädt Benutzer für Spring Security aus der {@link AppUser}-Tabelle.
 *
 * @author Robin
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    /**
     * Erzeugt den Service mit dem Benutzer-Repository.
     *
     * @param users das Benutzer-Repository
     */
    public DbUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Sucht den Benutzer anhand des Benutzernamens und bildet ihn auf
     * Spring Securitys {@link UserDetails} ab (Passwort-Hash, Rolle,
     * Aktiv-Status).
     *
     * @param username der Benutzername
     * @return die UserDetails für die Authentifizierung
     * @throws UsernameNotFoundException wenn der Benutzer nicht existiert
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .roles(u.getRole().name())
                .disabled(!u.isEnabled())
                .build();
    }
}
