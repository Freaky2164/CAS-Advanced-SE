package de.frauenhaus.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Nils
 *
 * Zahlungseingang zu einem {@link Bussgeld} (alt: frauenhaus.eingang).
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

    public Long getId() { return id; }
    public Bussgeld getBussgeld() { return bussgeld; }
    public void setBussgeld(Bussgeld bussgeld) { this.bussgeld = bussgeld; }
    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }
    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }
    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
}
