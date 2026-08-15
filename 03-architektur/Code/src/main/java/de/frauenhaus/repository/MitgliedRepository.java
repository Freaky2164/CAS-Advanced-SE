package de.frauenhaus.repository;

import de.frauenhaus.domain.Mitglied;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Datenzugriff für {@link Mitglied}, mit Abfragen für Verteiler,
 * E-Mail-Rundschreiben und Spendenquittungs-Empfänger.
 *
 * @author Ole
 */
public interface MitgliedRepository extends JpaRepository<Mitglied, Long> {

    /**
     * Sucht Mitglieder über die wichtigsten Textfelder der Mitgliederliste.
     *
     * @param suche der Suchbegriff (Teilstring, Groß-/Kleinschreibung egal)
     * @param pageable die gewünschte Seite und Sortierung
     * @return die passenden Mitglieder seitenweise
     */
    @Query(value = """
            SELECT DISTINCT m FROM Mitglied m
            LEFT JOIN m.stichworte s
            WHERE LOWER(COALESCE(m.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(m.vorname, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(m.name2, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(m.name3, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(m.ort, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(m.email, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT m.id) FROM Mitglied m
                    LEFT JOIN m.stichworte s
                    WHERE LOWER(COALESCE(m.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(m.vorname, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(m.name2, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(m.name3, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(m.ort, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(m.email, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                    """)
    Page<Mitglied> suchen(@Param("suche") String suche, Pageable pageable);

    /**
     * Liefert die Serienbrief-Adressaten zu den angegebenen Verteiler-Stichworten.
     *
     * @param stichworte die Namen der Stichworte
     * @return die Mitglieder sortiert nach Name und Vorname
     */
    @Query("""
            SELECT DISTINCT m FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte
            ORDER BY m.name, m.vorname""")
    List<Mitglied> findVerteiler(@Param("stichworte") Collection<String> stichworte);

    /**
     * Liefert die E-Mail-Adressen des Verteilers zu den angegebenen Stichworten.
     *
     * @param stichworte die Namen der Stichworte
     * @return die E-Mail-Adressen sortiert und ohne Duplikate
     */
    @Query("""
            SELECT DISTINCT m.email FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte AND m.email IS NOT NULL
            ORDER BY m.email""")
    List<String> findVerteilerEmails(@Param("stichworte") Collection<String> stichworte);

    /**
     * Sucht Mitglieder über ein oder mehrere Stichworte, optional zusätzlich
     * auf Förderverein- bzw. Frauenhaus-Mitglieder eingeschränkt.
     *
     * @param stichworte die Namen der Stichworte
     * @param foerderverein wenn {@code true}, nur Förderverein-Mitglieder
     * @param frauenhaus wenn {@code true}, nur Frauenhaus-Mitglieder
     * @return die Mitglieder sortiert nach Name und Vorname
     */
    @Query("""
            SELECT DISTINCT m FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte
              AND (:foerderverein = false OR m.foerderverein = true)
              AND (:frauenhaus = false OR m.frauenhaus = true)
            ORDER BY m.name, m.vorname""")
    List<Mitglied> findByStichwortSuche(@Param("stichworte") Collection<String> stichworte,
                                         @Param("foerderverein") boolean foerderverein,
                                         @Param("frauenhaus") boolean frauenhaus);
}
