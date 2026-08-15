package de.frauenhaus.repository;

import de.frauenhaus.domain.Verein;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Datenzugriff für {@link Verein}.
 *
 * @author Ole
 */
public interface VereinRepository extends JpaRepository<Verein, String> {

    /**
     * Sucht Vereine über Kurzname und Bezeichnung.
     *
     * @param suche der Suchbegriff (Teilstring, Groß-/Kleinschreibung egal)
     * @return die passenden Vereine
     */
    @Query("""
            SELECT v FROM Verein v
            WHERE LOWER(COALESCE(v.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(v.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
            """)
    List<Verein> suchen(@Param("suche") String suche);
}
