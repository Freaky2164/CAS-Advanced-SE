package de.frauenhaus.repository;

import de.frauenhaus.domain.Stichwort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Stichwort}, mit nativen Änderungsabfragen zum
 * Zusammenstellen bzw. Zusammenfassen von Verteiler-Stichworten.
 */
public interface StichwortRepository extends JpaRepository<Stichwort, String> {

    /**
     * Verteiler zusammenstellen: alle Mitglieder der alten Stichworte dem neuen
     * Stichwort zuordnen (alt: CReportStichwortZusammenstellen – jetzt parameterisiert).
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

    /** Alte Zuordnungen nach Zusammenfassen entfernen (alt: CReportStichworteZusammenfassen). */
    @Modifying
    @Query(value = "DELETE FROM frauenhaus.stichwort_person WHERE stichwort IN (:alte)",
            nativeQuery = true)
    int zuordnungenLoeschen(@Param("alte") Collection<String> alte);

    /** Alte Stichworte nach Zusammenfassen endgültig löschen. */
    @Modifying
    @Query(value = "DELETE FROM frauenhaus.stichwort WHERE stichwort IN (:alte)",
            nativeQuery = true)
    int stichworteLoeschen(@Param("alte") Collection<String> alte);
}
