package de.frauenhaus.repository;

import de.frauenhaus.domain.Anrede;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Anrede}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 */
public interface AnredeRepository extends JpaRepository<Anrede, String> {
}
