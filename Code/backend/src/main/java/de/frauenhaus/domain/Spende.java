package de.frauenhaus.domain;

import jakarta.persistence.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Spende eines {@link Mitglied}s an einen {@link Verein}, klassifiziert nach
 * {@link Spendenart}.
 *
 * @author Robin
 */
@Entity
@Table(name = "spende", schema = "frauenhaus")
@Audited
@AuditTable(value = "spende_aud", schema = "frauenhaus")
public class Spende {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spende")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mitglied")
    private Mitglied mitglied;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(optional = false)
    @JoinColumn(name = "spendenart")
    private Spendenart spendenart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "verein")
    private Verein verein;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private BigDecimal betrag;

    private String bemerkung;

    /** Liefert die ID der Spende. */
    public Long getId() { return id; }

    /** Liefert das spendende Mitglied. */
    public Mitglied getMitglied() { return mitglied; }

    /** Setzt das spendende Mitglied. */
    public void setMitglied(Mitglied mitglied) { this.mitglied = mitglied; }

    /** Liefert die Spendenart. */
    public Spendenart getSpendenart() { return spendenart; }

    /** Setzt die Spendenart. */
    public void setSpendenart(Spendenart spendenart) { this.spendenart = spendenart; }

    /** Liefert den begünstigten Verein. */
    public Verein getVerein() { return verein; }

    /** Setzt den begünstigten Verein. */
    public void setVerein(Verein verein) { this.verein = verein; }

    /** Liefert das Spendendatum. */
    public LocalDate getDatum() { return datum; }

    /** Setzt das Spendendatum. */
    public void setDatum(LocalDate datum) { this.datum = datum; }

    /** Liefert den gespendeten Betrag. */
    public BigDecimal getBetrag() { return betrag; }

    /** Setzt den gespendeten Betrag. */
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    /** Liefert die Bemerkung. */
    public String getBemerkung() { return bemerkung; }

    /** Setzt die Bemerkung. */
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
}
