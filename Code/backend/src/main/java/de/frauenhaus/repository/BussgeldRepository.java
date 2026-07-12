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
 * @author Nils
 *
 * Datenzugriff für {@link Bussgeld}, inklusive der nativen Reporting-Abfragen
 * für die Bußgeld-Übersicht und -Detailansicht.
 */
public interface BussgeldRepository extends JpaRepository<Bussgeld, Long> {

    /** Generische Suche über die wichtigsten Textfelder der Bußgeldliste. */
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

    /** Zeile der Bußgeld-Übersicht: Summen je Gericht (alt: CReportBussgeldUebersicht.compute). */
    interface UebersichtZeile {
        String getBezeichnung();
        BigDecimal getBussgelder();
        BigDecimal getEingaenge();
    }

    /** Summen der Bußgelder und Zahlungseingänge je Gericht im Zeitraum, für einen Träger. */
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

    /** Bußgelder mit Zahlungseingängen im Zeitraum (alt: CReportBussgeldDetail). */
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
