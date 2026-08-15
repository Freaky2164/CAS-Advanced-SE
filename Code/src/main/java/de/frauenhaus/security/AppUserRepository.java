package de.frauenhaus.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Datenzugriff für {@link AppUser}.
 *
 * @author Paul
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Sucht einen Benutzer anhand seines eindeutigen Benutzernamens.
     *
     * @param username der Benutzername
     * @return der Benutzer, falls vorhanden
     */
    Optional<AppUser> findByUsername(String username);
}
