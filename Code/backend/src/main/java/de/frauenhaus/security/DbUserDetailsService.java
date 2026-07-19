package de.frauenhaus.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author Nils
 *     <p>Lädt Benutzer für Spring Security aus der {@link AppUser}-Tabelle, anstatt sie in-memory
 *     oder statisch zu konfigurieren.
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;

  public DbUserDetailsService(AppUserRepository users) {
    this.users = users;
  }

  /**
   * @author Nils
   *     <p>Sucht den Benutzer über {@link AppUserRepository#findByUsername(String)} und bildet ihn
   *     auf Spring Securitys {@link UserDetails} ab (Passwort-Hash, Rolle, aktiviert-Flag).
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AppUser u =
        users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
    return User.withUsername(u.getUsername())
        .password(u.getPasswordHash())
        .roles(u.getRole().name())
        .disabled(!u.isEnabled())
        .build();
  }
}
