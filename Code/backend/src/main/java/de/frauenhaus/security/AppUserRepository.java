package de.frauenhaus.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Nils
 *     <p>Datenzugriff für {@link AppUser}.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  /**
   * @author Nils
   *     <p>Sucht einen Benutzer anhand seines eindeutigen Benutzernamens.
   */
  Optional<AppUser> findByUsername(String username);
}
