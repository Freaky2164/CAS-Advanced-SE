package de.frauenhaus.domain;

import jakarta.persistence.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Nils
 *
 * Spende eines {@link Mitglied}s an einen {@link Verein}, klassifiziert nach
 * {@link Spendenart} (alt: frauenhaus.spende).
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

    public Long getId() { return id; }
    public Mitglied getMitglied() { return mitglied; }
    public void setMitglied(Mitglied mitglied) { this.mitglied = mitglied; }
    public Spendenart getSpendenart() { return spendenart; }
    public void setSpendenart(Spendenart spendenart) { this.spendenart = spendenart; }
    public Verein getVerein() { return verein; }
    public void setVerein(Verein verein) { this.verein = verein; }
    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }
    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }
    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
}
