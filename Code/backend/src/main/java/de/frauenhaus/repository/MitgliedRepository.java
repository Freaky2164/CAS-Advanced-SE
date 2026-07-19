package de.frauenhaus.repository;

import de.frauenhaus.domain.Mitglied;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Nils
 *     <p>Datenzugriff für {@link Mitglied}, mit Abfragen für Verteiler, E-Mail-Rundschreiben und
 *     Spendenquittungs-Empfänger.
 */
public interface MitgliedRepository extends JpaRepository<Mitglied, Long> {

  /**
   * @author Nils
   *     <p>Generische Suche über die wichtigsten Textfelder der Mitgliederliste (alt: Filter im
   *     generischen Listen-Frame).
   */
  @Query(
      value =
          """
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
      countQuery =
          """
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
   * @author Nils
   *     <p>Serienbrief-Adressen für Verteiler-Stichworte (alt: CReportSerienbriefAdressen – jetzt
   *     parameterisiert statt String-Konkatenation).
   */
  @Query(
      """
            SELECT DISTINCT m FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte
            ORDER BY m.name, m.vorname""")
  List<Mitglied> findVerteiler(@Param("stichworte") Collection<String> stichworte);

  /**
   * @author Nils
   *     <p>E-Mail-Verteiler (alt: CReportVerteiler).
   */
  @Query(
      """
            SELECT DISTINCT m.email FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte AND m.email IS NOT NULL
            ORDER BY m.email""")
  List<String> findVerteilerEmails(@Param("stichworte") Collection<String> stichworte);

  /**
   * @author Nils
   *     <p>Empfänger einer Spendenquittung (alt: CCommandSpendenQuittung).
   */
  @Query("SELECT DISTINCT s.mitglied FROM Spende s WHERE s.id = :spendeId")
  List<Mitglied> findBySpende(@Param("spendeId") Long spendeId);

  /**
   * @author Nils
   *     <p>Mitgliedersuche über ein oder mehrere Stichworte, optional zusätzlich auf
   *     Förderverein-/Frauenhaus-Mitglieder eingeschränkt (alt: CReportStichwortSuche).
   */
  @Query(
      """
            SELECT DISTINCT m FROM Mitglied m JOIN m.stichworte s
            WHERE s.name IN :stichworte
              AND (:foerderverein = false OR m.foerderverein = true)
              AND (:frauenhaus = false OR m.frauenhaus = true)
            ORDER BY m.name, m.vorname""")
  List<Mitglied> findByStichwortSuche(
      @Param("stichworte") Collection<String> stichworte,
      @Param("foerderverein") boolean foerderverein,
      @Param("frauenhaus") boolean frauenhaus);
}
