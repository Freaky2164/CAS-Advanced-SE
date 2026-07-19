package de.frauenhaus.repository;

import de.frauenhaus.domain.Spendenart;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Nils
 *     <p>Datenzugriff für {@link Spendenart}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 */
public interface SpendenartRepository extends JpaRepository<Spendenart, String> {}
