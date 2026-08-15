package de.frauenhaus.repository;

import de.frauenhaus.domain.Spendentyp;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für {@link Spendentyp}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 *
 * @author Ole
 */
public interface SpendentypRepository extends JpaRepository<Spendentyp, String> {
}
