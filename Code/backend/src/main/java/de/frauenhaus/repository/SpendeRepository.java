package de.frauenhaus.repository;

import de.frauenhaus.domain.Spende;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Datenzugriff für {@link Spende}, mit Abfragen für die Jahresübersicht und
 * für die Summenbildung bei Dauerspenden-Quittungen.
 *
 * @author Nils
 */
public interface SpendeRepository extends JpaRepository<Spende, Long> {

    /**
     * Sucht Spenden über die wichtigsten Textfelder der Spendenliste.
     *
     * @param suche der Suchbegriff (Teilstring, Groß-/Kleinschreibung egal)
     * @param pageable die gewünschte Seite und Sortierung
     * @return die passenden Spenden seitenweise
     */
    @Query(value = """
            SELECT s FROM Spende s
            JOIN s.mitglied m
            JOIN s.spendenart sa
            JOIN s.verein v
            WHERE LOWER(COALESCE(m.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(m.vorname, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(sa.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(sa.spendentyp, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(v.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(v.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(s.bemerkung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
            """,
            countQuery = """
                    SELECT COUNT(s) FROM Spende s
                    JOIN s.mitglied m
                    JOIN s.spendenart sa
                    JOIN s.verein v
                    WHERE LOWER(COALESCE(m.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(m.vorname, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(sa.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(sa.spendentyp, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(v.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(v.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(s.bemerkung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                    """)
    Page<Spende> suchen(@Param("suche") String suche, Pageable pageable);

    /**
     * Liefert alle Spenden eines Jahres für die Spendenübersicht.
     *
     * @param jahr das Kalenderjahr
     * @return die Spenden sortiert nach Träger, Spendentyp, Spendenart und Spender
     */
    @Query("""
            SELECT s FROM Spende s
            WHERE YEAR(s.datum) = :jahr
            ORDER BY s.verein.name, s.spendenart.spendentyp, s.spendenart.name,
                     s.mitglied.name, s.mitglied.vorname, s.datum""")
    List<Spende> findUebersicht(@Param("jahr") int jahr);

    /**
     * Liefert alle Einzelspenden eines Mitglieds im Jahr für einen Spendentyp
     * und Träger, als Grundlage der Summenbildung bei Dauerspenden-Quittungen.
     *
     * @param mitgliedId die ID des Mitglieds
     * @param jahr das Kalenderjahr
     * @param spendentyp die Bezeichnung des Spendentyps
     * @param verein der Kurzname des Trägervereins
     * @return die Spenden sortiert nach Datum
     */
    @Query("""
            SELECT s FROM Spende s
            WHERE s.mitglied.id = :mitgliedId
              AND YEAR(s.datum) = :jahr
              AND s.spendenart.spendentyp = :spendentyp
              AND s.verein.name = :verein
            ORDER BY s.datum""")
    List<Spende> findJahresspenden(@Param("mitgliedId") Long mitgliedId,
                                   @Param("jahr") int jahr,
                                   @Param("spendentyp") String spendentyp,
                                   @Param("verein") String verein);
}
