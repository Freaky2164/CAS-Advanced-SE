package de.frauenhaus.repository;

import de.frauenhaus.domain.Bussgeldstatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für {@link Bussgeldstatus}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 *
 * @author Nils
 */
public interface BussgeldstatusRepository extends JpaRepository<Bussgeldstatus, String> {
}
