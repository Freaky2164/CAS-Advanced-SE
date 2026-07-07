package de.frauenhaus.repository;

import de.frauenhaus.domain.Spende;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Spende}, mit Abfragen für die Jahresübersicht
 * und für die Summenbildung bei Dauerspenden-Quittungen.
 */
public interface SpendeRepository extends JpaRepository<Spende, Long> {

    /** Spendenübersicht eines Jahres (alt: CReportSpendenUebersicht). */
    @Query("""
            SELECT s FROM Spende s
            WHERE YEAR(s.datum) = :jahr
            ORDER BY s.verein.name, s.spendenart.spendentyp, s.spendenart.name,
                     s.mitglied.name, s.mitglied.vorname, s.datum""")
    List<Spende> findUebersicht(@Param("jahr") int jahr);

    /**
     * Alle Einzelspenden eines Mitglieds im Jahr für einen Spendentyp/Träger –
     * Summenbildung für Dauerspenden-Quittungen (alt: fillDonationSummary).
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
