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
 * @author Nils
 *     <p>Dokument-Anhang zu einem Stammdatensatz (alt: CDisplayFieldDocumentBean). Die Zuordnung
 *     erfolgt polymorph über Entity-Typ + Entity-ID.
 */
@Entity
@Table(
    name = "dokument",
    schema = "frauenhaus",
    indexes = {@Index(name = "idx_dokument_entity", columnList = "entity_typ, entity_id")})
public class Dokument {

  public enum EntityTyp {
    MITGLIED,
    VEREIN,
    BUSSGELD,
    SPENDE,
    GERICHT
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

  public Long getId() {
    return id;
  }

  public EntityTyp getEntityTyp() {
    return entityTyp;
  }

  public void setEntityTyp(EntityTyp entityTyp) {
    this.entityTyp = entityTyp;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getDateiname() {
    return dateiname;
  }

  public void setDateiname(String dateiname) {
    this.dateiname = dateiname;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public long getGroesse() {
    return groesse;
  }

  public void setGroesse(long groesse) {
    this.groesse = groesse;
  }

  public byte[] getInhalt() {
    return inhalt;
  }

  public void setInhalt(byte[] inhalt) {
    this.inhalt = inhalt;
  }

  public OffsetDateTime getHochgeladenAm() {
    return hochgeladenAm;
  }

  public void setHochgeladenAm(OffsetDateTime hochgeladenAm) {
    this.hochgeladenAm = hochgeladenAm;
  }

  public String getHochgeladenVon() {
    return hochgeladenVon;
  }

  public void setHochgeladenVon(String hochgeladenVon) {
    this.hochgeladenVon = hochgeladenVon;
  }
}
