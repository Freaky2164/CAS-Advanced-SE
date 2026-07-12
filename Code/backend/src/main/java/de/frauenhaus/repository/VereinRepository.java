package de.frauenhaus.repository;

import de.frauenhaus.domain.Verein;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Verein} (Träger: 'Frauenhaus'/'Förderverein'). Reine
 * CRUD-Operationen, keine zusätzlichen Abfragen.
 */
public interface VereinRepository extends JpaRepository<Verein, String> {

    /** Generische Suche über Kürzel und Bezeichnung der Vereine. */
    @Query("""
            SELECT v FROM Verein v
            WHERE LOWER(COALESCE(v.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(v.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
            """)
    List<Verein> suchen(@Param("suche") String suche);
}
