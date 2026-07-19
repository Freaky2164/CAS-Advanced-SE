package de.frauenhaus.repository;

import de.frauenhaus.domain.Bussgeld;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datenzugriff für {@link Bussgeld}, inklusive der Reporting-Abfragen für die
 * Bußgeld-Übersicht und -Detailansicht.
 *
 * @author Ole
 */
public interface BussgeldRepository extends JpaRepository<Bussgeld, Long> {

    /**
     * Sucht Bußgelder über die wichtigsten Textfelder der Bußgeldliste.
     *
     * @param suche der Suchbegriff (Teilstring, Groß-/Kleinschreibung egal)
     * @param pageable die gewünschte Seite und Sortierung
     * @return die passenden Bußgelder seitenweise
     */
    @Query(value = """
            SELECT b FROM Bussgeld b
            JOIN b.gericht g
            JOIN b.verein v
            WHERE LOWER(COALESCE(b.status, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(b.vorname, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(b.aktenzeichen, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(b.bemerkung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(g.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(v.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
               OR LOWER(COALESCE(v.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
            """,
            countQuery = """
                    SELECT COUNT(b) FROM Bussgeld b
                    JOIN b.gericht g
                    JOIN b.verein v
                    WHERE LOWER(COALESCE(b.status, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(b.vorname, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(b.aktenzeichen, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(b.bemerkung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(g.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(v.name, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                       OR LOWER(COALESCE(v.bezeichnung, '')) LIKE LOWER(CONCAT('%', :suche, '%'))
                    """)
    Page<Bussgeld> suchen(@Param("suche") String suche, Pageable pageable);

    /**
     * Zeile der Bußgeld-Übersicht mit den Summen je Gericht.
     */
    interface UebersichtZeile {
        /** Liefert die Bezeichnung des Gerichts. */
        String getBezeichnung();

        /** Liefert die Summe der zugewiesenen Bußgelder. */
        BigDecimal getBussgelder();

        /** Liefert die Summe der Zahlungseingänge. */
        BigDecimal getEingaenge();
    }

    /**
     * Ermittelt die Summen der Bußgelder und Zahlungseingänge je Gericht im
     * Zeitraum für einen Träger.
     *
     * @param von Beginn des Zeitraums (einschließlich)
     * @param bis Ende des Zeitraums (einschließlich)
     * @param verein der Kurzname des Trägervereins
     * @return eine Übersichtszeile je Gericht
     */
    @Query(value = """
            SELECT g.bezeichnung AS bezeichnung,
                   coalesce((SELECT sum(b.betrag) FROM frauenhaus.bussgeld b
                             WHERE b.gericht = g.gericht
                               AND b.datum BETWEEN :von AND :bis
                               AND b.verein = :verein), 0) AS bussgelder,
                   coalesce((SELECT sum(e.betrag)
                             FROM frauenhaus.eingang e
                             JOIN frauenhaus.bussgeld b2 ON b2.bussgeld = e.bussgeld
                             WHERE b2.gericht = g.gericht
                               AND e.datum BETWEEN :von AND :bis
                               AND b2.verein = :verein), 0) AS eingaenge
            FROM frauenhaus.gericht g
            ORDER BY g.bezeichnung""", nativeQuery = true)
    List<UebersichtZeile> uebersicht(@Param("von") LocalDate von,
                                     @Param("bis") LocalDate bis,
                                     @Param("verein") String verein);

    /**
     * Liefert alle Bußgelder eines Trägers mit Zahlungseingängen im Zeitraum.
     *
     * @param von Beginn des Zeitraums (einschließlich)
     * @param bis Ende des Zeitraums (einschließlich)
     * @param verein der Kurzname des Trägervereins
     * @return die Bußgelder sortiert nach Datum und Aktenzeichen
     */
    @Query("""
            SELECT DISTINCT b FROM Bussgeld b
            JOIN b.eingaenge e
            WHERE e.datum BETWEEN :von AND :bis
              AND b.verein.name = :verein
            ORDER BY b.datum, b.aktenzeichen""")
    List<Bussgeld> findMitEingaengen(@Param("von") LocalDate von,
                                     @Param("bis") LocalDate bis,
                                     @Param("verein") String verein);
}
