package de.frauenhaus.repository;

import de.frauenhaus.domain.Dokument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Datenzugriff für {@link Dokument}, inklusive Metadaten-Abfrage ohne Laden
 * des eigentlichen Dateiinhalts.
 *
 * @author Robin
 */
public interface DokumentRepository extends JpaRepository<Dokument, Long> {

    /**
     * Projektion der Dokument-Metadaten ohne den Dateiinhalt.
     */
    interface DokumentMetadatenProjection {
        /** Liefert die ID des Dokuments. */
        Long getId();

        /** Liefert den Typ des zugeordneten Stammdatensatzes. */
        Dokument.EntityTyp getEntityTyp();

        /** Liefert die ID des zugeordneten Stammdatensatzes. */
        String getEntityId();

        /** Liefert den Dateinamen. */
        String getDateiname();

        /** Liefert den MIME-Typ des Inhalts. */
        String getContentType();

        /** Liefert die Dateigröße in Bytes. */
        long getGroesse();

        /** Liefert den Zeitpunkt des Uploads. */
        OffsetDateTime getHochgeladenAm();

        /** Liefert den Benutzernamen des Hochladenden. */
        String getHochgeladenVon();
    }

    /**
     * Liefert alle Dokumente eines Stammdatensatzes, neueste zuerst.
     *
     * @param entityTyp der Typ des Stammdatensatzes
     * @param entityId die ID des Stammdatensatzes
     * @return die Dokumente inklusive Inhalt
     */
    List<Dokument> findByEntityTypAndEntityIdOrderByHochgeladenAmDesc(Dokument.EntityTyp entityTyp, String entityId);

    /**
     * Liefert die Metadaten aller Dokumente eines Stammdatensatzes, neueste
     * zuerst, ohne den Dateiinhalt zu laden.
     *
     * @param entityTyp der Typ des Stammdatensatzes
     * @param entityId die ID des Stammdatensatzes
     * @return die Metadaten-Projektionen
     */
    @Query("""
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
            @Param("entityTyp") Dokument.EntityTyp entityTyp,
            @Param("entityId") String entityId);

    /**
     * Löscht alle Dokumente eines Stammdatensatzes.
     *
     * @param entityTyp der Typ des Stammdatensatzes
     * @param entityId die ID des Stammdatensatzes
     */
    void deleteByEntityTypAndEntityId(Dokument.EntityTyp entityTyp, String entityId);
}
