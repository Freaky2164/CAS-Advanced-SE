package de.frauenhaus.repository;

import de.frauenhaus.domain.Gericht;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Gericht}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 */
public interface GerichtRepository extends JpaRepository<Gericht, Long> {
}
