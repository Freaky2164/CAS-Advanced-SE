package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Dokument-Anhang zu einem Stammdatensatz. Die Zuordnung erfolgt polymorph
 * über Entity-Typ und Entity-ID.
 *
 * @author Robin
 */
@Entity
@Table(name = "dokument", schema = "frauenhaus", indexes = {
        @Index(name = "idx_dokument_entity", columnList = "entity_typ, entity_id")
})
public class Dokument {

    /**
     * Typ des Stammdatensatzes, dem ein Dokument zugeordnet ist.
     */
    public enum EntityTyp {
        MITGLIED, VEREIN, BUSSGELD, SPENDE, GERICHT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_typ", nullable = false, length = 20)
    private EntityTyp entityTyp;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(nullable = false, length = 255)
    private String dateiname;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long groesse;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] inhalt;

    @Column(name = "hochgeladen_am", nullable = false, updatable = false)
    private OffsetDateTime hochgeladenAm;

    @Column(name = "hochgeladen_von", nullable = false, length = 50)
    private String hochgeladenVon;

    /** Liefert die ID des Dokuments. */
    public Long getId() { return id; }

    /** Liefert den Typ des zugeordneten Stammdatensatzes. */
    public EntityTyp getEntityTyp() { return entityTyp; }

    /** Setzt den Typ des zugeordneten Stammdatensatzes. */
    public void setEntityTyp(EntityTyp entityTyp) { this.entityTyp = entityTyp; }

    /** Liefert die ID des zugeordneten Stammdatensatzes. */
    public String getEntityId() { return entityId; }

    /** Setzt die ID des zugeordneten Stammdatensatzes. */
    public void setEntityId(String entityId) { this.entityId = entityId; }

    /** Liefert den Dateinamen. */
    public String getDateiname() { return dateiname; }

    /** Setzt den Dateinamen. */
    public void setDateiname(String dateiname) { this.dateiname = dateiname; }

    /** Liefert den MIME-Typ des Inhalts. */
    public String getContentType() { return contentType; }

    /** Setzt den MIME-Typ des Inhalts. */
    public void setContentType(String contentType) { this.contentType = contentType; }

    /** Liefert die Dateigröße in Bytes. */
    public long getGroesse() { return groesse; }

    /** Setzt die Dateigröße in Bytes. */
    public void setGroesse(long groesse) { this.groesse = groesse; }

    /** Liefert den Dateiinhalt. */
    public byte[] getInhalt() { return inhalt; }

    /** Setzt den Dateiinhalt. */
    public void setInhalt(byte[] inhalt) { this.inhalt = inhalt; }

    /** Liefert den Zeitpunkt des Uploads. */
    public OffsetDateTime getHochgeladenAm() { return hochgeladenAm; }

    /** Setzt den Zeitpunkt des Uploads. */
    public void setHochgeladenAm(OffsetDateTime hochgeladenAm) { this.hochgeladenAm = hochgeladenAm; }

    /** Liefert den Benutzernamen des Hochladenden. */
    public String getHochgeladenVon() { return hochgeladenVon; }

    /** Setzt den Benutzernamen des Hochladenden. */
    public void setHochgeladenVon(String hochgeladenVon) { this.hochgeladenVon = hochgeladenVon; }
}
