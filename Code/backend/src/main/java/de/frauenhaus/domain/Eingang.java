package de.frauenhaus.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Zahlungseingang zu einem {@link Bussgeld}.
 *
 * @author Paul
 */
@Entity
@Table(name = "eingang", schema = "frauenhaus")
public class Eingang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eingang")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bussgeld")
    private Bussgeld bussgeld;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private BigDecimal betrag;

    private String bemerkung;

    /** Liefert die ID des Zahlungseingangs. */
    public Long getId() { return id; }

    /** Liefert das zugehörige Bußgeld. */
    public Bussgeld getBussgeld() { return bussgeld; }

    /** Setzt das zugehörige Bußgeld. */
    public void setBussgeld(Bussgeld bussgeld) { this.bussgeld = bussgeld; }

    /** Liefert das Eingangsdatum. */
    public LocalDate getDatum() { return datum; }

    /** Setzt das Eingangsdatum. */
    public void setDatum(LocalDate datum) { this.datum = datum; }

    /** Liefert den eingegangenen Betrag. */
    public BigDecimal getBetrag() { return betrag; }

    /** Setzt den eingegangenen Betrag. */
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    /** Liefert die Bemerkung. */
    public String getBemerkung() { return bemerkung; }

    /** Setzt die Bemerkung. */
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
}
