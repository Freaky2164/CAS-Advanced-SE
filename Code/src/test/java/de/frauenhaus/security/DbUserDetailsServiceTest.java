package de.frauenhaus.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests der Abbildung von {@link AppUser} auf Spring Securitys
 * {@link UserDetails}.
 *
 * @author Paul
 */
@ExtendWith(MockitoExtension.class)
class DbUserDetailsServiceTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuv";

    @Mock
    private AppUserRepository users;

    @InjectMocks
    private DbUserDetailsService service;

    private static AppUser benutzer(AppUser.Role rolle, boolean aktiv)
    {
        AppUser u = new AppUser("ole", HASH, rolle);
        u.setEnabled(aktiv);
        return u;
    }

    @Test
    @DisplayName("Rolle wird als ROLE_-Autorität abgebildet")
    void loadUserByUsername_sachbearbeitung_liefertRolePraefix()
    {
        when(users.findByUsername("ole")).thenReturn(Optional.of(benutzer(AppUser.Role.SACHBEARBEITUNG, true)));

        UserDetails details = service.loadUserByUsername("ole");

        assertThat(details.getUsername()).isEqualTo("ole");
        assertThat(details.getPassword()).isEqualTo(HASH);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SACHBEARBEITUNG");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("ADMIN erhält ROLE_ADMIN")
    void loadUserByUsername_admin_liefertRoleAdmin()
    {
        when(users.findByUsername("chef")).thenReturn(Optional.of(new AppUser("chef", HASH, AppUser.Role.ADMIN)));

        assertThat(service.loadUserByUsername("chef").getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("deaktivierter Benutzer wird als disabled gemeldet")
    void loadUserByUsername_deaktiviert_istDisabled()
    {
        when(users.findByUsername("ole")).thenReturn(Optional.of(benutzer(AppUser.Role.SACHBEARBEITUNG, false)));

        assertThat(service.loadUserByUsername("ole").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("unbekannter Benutzer -> UsernameNotFoundException")
    void loadUserByUsername_unbekannt_wirftException()
    {
        when(users.findByUsername("gibtsnicht")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("gibtsnicht"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("gibtsnicht");
    }
}
