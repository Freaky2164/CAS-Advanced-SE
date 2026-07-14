package de.frauenhaus.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * @author Nils
 *
 * Legt beim allerersten Start einen Admin an, falls noch kein Benutzer existiert.
 * Passwort aus APP_ADMIN_PASSWORD, sonst Zufallswert (wird einmalig geloggt).
 * Es wird nie ein Standardpasswort eingebrannt.
 */
@Configuration
public class AdminBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

    /**
     * @author Nils
     *
     * Erstellt beim Anwendungsstart genau dann einen Admin-Benutzer, wenn die
     * Benutzertabelle noch leer ist. Nutzt {@code app.security.initial-admin-password},
     * falls gesetzt, andernfalls ein zufällig generiertes Einmal-Passwort.
     */
    @Bean
    ApplicationRunner createInitialAdmin(AppUserRepository users,
                                         PasswordEncoder encoder,
                                         @Value("${app.security.initial-admin-password:}") String configured) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            String password = configured == null || configured.isBlank()
                    ? UUID.randomUUID().toString()
                    : configured;
            users.save(new AppUser("admin", encoder.encode(password), AppUser.Role.ADMIN));
            if (configured == null || configured.isBlank()) {
                LOG.warn("Initialer Admin 'admin' angelegt. Einmal-Passwort: {} – sofort ändern!", password);
            } else {
                LOG.info("Initialer Admin 'admin' aus APP_ADMIN_PASSWORD angelegt.");
            }
        };
    }
}
