package de.frauenhaus.repository;

import de.frauenhaus.domain.Anrede;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für {@link Anrede}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 *
 * @author Ole
 */
public interface AnredeRepository extends JpaRepository<Anrede, String> {
}
