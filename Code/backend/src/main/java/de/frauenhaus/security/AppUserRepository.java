package de.frauenhaus.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Nils
 *
 * Datenzugriff für {@link AppUser}.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * @author Nils
     *
     * Sucht einen Benutzer anhand seines eindeutigen Benutzernamens.
     */
    Optional<AppUser> findByUsername(String username);
}
