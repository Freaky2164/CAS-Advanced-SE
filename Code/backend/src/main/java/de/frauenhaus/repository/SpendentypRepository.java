package de.frauenhaus.repository;

import de.frauenhaus.domain.Spendentyp;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Spendentyp}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 */
public interface SpendentypRepository extends JpaRepository<Spendentyp, String> {
}
