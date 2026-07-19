package de.frauenhaus.repository;

import de.frauenhaus.domain.Stichwort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * Datenzugriff für {@link Stichwort}, mit Änderungsabfragen zum
 * Zusammenstellen bzw. Zusammenfassen von Verteiler-Stichworten.
 *
 * @author Ole
 */
public interface StichwortRepository extends JpaRepository<Stichwort, String> {

    /**
     * Ordnet alle Mitglieder der alten Stichworte dem neuen Stichwort zu,
     * sofern die Zuordnung nicht bereits besteht.
     *
     * @param neu der Name des neuen Stichworts
     * @param alte die Namen der zusammenzufassenden Stichworte
     * @return die Anzahl der angelegten Zuordnungen
     */
    @Modifying
    @Query(value = """
            INSERT INTO frauenhaus.stichwort_person (mitglied, stichwort)
            SELECT DISTINCT sp.mitglied, :neu
            FROM frauenhaus.stichwort_person sp
            WHERE sp.stichwort IN (:alte)
              AND sp.mitglied NOT IN (SELECT mitglied FROM frauenhaus.stichwort_person
                                      WHERE stichwort = :neu)""", nativeQuery = true)
    int stichworteZuordnen(@Param("neu") String neu, @Param("alte") Collection<String> alte);

    /**
     * Entfernt die Mitglieder-Zuordnungen der angegebenen Stichworte.
     *
     * @param alte die Namen der Stichworte
     * @return die Anzahl der gelöschten Zuordnungen
     */
    @Modifying
    @Query(value = "DELETE FROM frauenhaus.stichwort_person WHERE stichwort IN (:alte)",
            nativeQuery = true)
    int zuordnungenLoeschen(@Param("alte") Collection<String> alte);

    /**
     * Löscht die angegebenen Stichworte endgültig.
     *
     * @param alte die Namen der Stichworte
     * @return die Anzahl der gelöschten Stichworte
     */
    @Modifying
    @Query(value = "DELETE FROM frauenhaus.stichwort WHERE stichwort IN (:alte)",
            nativeQuery = true)
    int stichworteLoeschen(@Param("alte") Collection<String> alte);
}
