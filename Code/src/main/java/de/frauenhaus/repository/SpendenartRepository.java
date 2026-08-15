package de.frauenhaus.repository;

import de.frauenhaus.domain.Spendenart;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für {@link Spendenart}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 *
 * @author Ole
 */
public interface SpendenartRepository extends JpaRepository<Spendenart, String> {
}
