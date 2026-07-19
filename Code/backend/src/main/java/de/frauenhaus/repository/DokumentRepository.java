package de.frauenhaus.repository;

import de.frauenhaus.domain.Dokument;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Nils
 *     <p>Datenzugriff für {@link Dokument} inkl. leichter Metadaten-Abfrage ohne Laden des
 *     eigentlichen Datei-Inhalts.
 */
public interface DokumentRepository extends JpaRepository<Dokument, Long> {

  interface DokumentMetadatenProjection {
    Long getId();

    Dokument.EntityTyp getEntityTyp();

    String getEntityId();

    String getDateiname();

    String getContentType();

    long getGroesse();

    OffsetDateTime getHochgeladenAm();

    String getHochgeladenVon();
  }

  List<Dokument> findByEntityTypAndEntityIdOrderByHochgeladenAmDesc(
      Dokument.EntityTyp entityTyp, String entityId);

  @Query(
      """
            SELECT d.id AS id,
                   d.entityTyp AS entityTyp,
                   d.entityId AS entityId,
                   d.dateiname AS dateiname,
                   d.contentType AS contentType,
                   d.groesse AS groesse,
                   d.hochgeladenAm AS hochgeladenAm,
                   d.hochgeladenVon AS hochgeladenVon
            FROM Dokument d
            WHERE d.entityTyp = :entityTyp
              AND d.entityId = :entityId
            ORDER BY d.hochgeladenAm DESC""")
  List<DokumentMetadatenProjection> findMetadatenByEntityTypAndEntityIdOrderByHochgeladenAmDesc(
      @Param("entityTyp") Dokument.EntityTyp entityTyp, @Param("entityId") String entityId);

  void deleteByEntityTypAndEntityId(Dokument.EntityTyp entityTyp, String entityId);
}
