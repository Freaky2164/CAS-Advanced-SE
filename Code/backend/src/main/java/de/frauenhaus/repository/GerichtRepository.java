package de.frauenhaus.repository;

import de.frauenhaus.domain.Gericht;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Gericht}. Reine CRUD-Operationen, keine zusätzlichen Abfragen.
 */
public interface GerichtRepository extends JpaRepository<Gericht, Long> {

    /**
     * @author Nils
     *
     * Generische Suche über die wichtigsten Textfelder der Gerichte.
     */
    @Query("""
            SELECT g FROM Gericht g
            WHERE LOWER(COALESCE(g.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(g.strasse, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(g.plz, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(g.ort, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
            """)
    List<Gericht> suchen(@Param("suche") String suche);
}
