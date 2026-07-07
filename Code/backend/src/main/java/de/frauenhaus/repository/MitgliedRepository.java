package de.frauenhaus.repository;

import de.frauenhaus.domain.Mitglied;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author Nils
 *
 * Datenzugriff für {@link Mitglied}, mit Abfragen für Verteiler,
 * E-Mail-Rundschreiben und Spendenquittungs-Empfänger.
 */
public interface MitgliedRepository extends JpaRepository<Mitglied, Long> {

    /**
     * Serienbrief-Adressen für Verteiler-Stichworte
     * (alt: CReportSerienbriefAdressen – jetzt parameterisiert statt String-Konkatenation).
     */
    @Query("""
            SELECT DISTINCT m FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte
            ORDER BY m.name, m.vorname""")
    List<Mitglied> findVerteiler(@Param("stichworte") Collection<String> stichworte);

    /** E-Mail-Verteiler (alt: CReportVerteiler). */
    @Query("""
            SELECT DISTINCT m.email FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte AND m.email IS NOT NULL
            ORDER BY m.email""")
    List<String> findVerteilerEmails(@Param("stichworte") Collection<String> stichworte);

    /** Empfänger einer Spendenquittung (alt: CCommandSpendenQuittung). */
    @Query("SELECT DISTINCT s.mitglied FROM Spende s WHERE s.id = :spendeId")
    List<Mitglied> findBySpende(@Param("spendeId") Long spendeId);
}
