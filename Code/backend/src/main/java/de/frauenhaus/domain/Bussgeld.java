package de.frauenhaus.domain;

import jakarta.persistence.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nils
 *
 * Bußgeld-Zuweisung durch ein {@link Gericht} an einen {@link Verein}
 * (alt: frauenhaus.bussgeld). Hält den geschuldeten Betrag sowie alle
 * zugehörigen {@link Eingang Zahlungseingänge}.
 */
@Entity
@Table(name = "bussgeld", schema = "frauenhaus")
@Audited
@AuditTable(value = "bussgeld_aud", schema = "frauenhaus")
public class Bussgeld {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bussgeld")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "gericht")
    private Gericht gericht;

    @ManyToOne(optional = false)
    @JoinColumn(name = "verein")
    private Verein verein;

    @Column(name = "status")
    private String status;

    private String name;
    private String vorname;
    private String aktenzeichen;

    @Column(nullable = false)
    private LocalDate datum;

    /** Zahlungsziel (alt: zieldatum). */
    private LocalDate zieldatum;

    @Column(nullable = false)
    private BigDecimal betrag;

    @Column(nullable = false)
    private boolean bezahlt;

    private String bemerkung;

    @NotAudited
    @OneToMany(mappedBy = "bussgeld", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Eingang> eingaenge = new ArrayList<>();

    public Long getId() { return id; }
    public Gericht getGericht() { return gericht; }
    public void setGericht(Gericht gericht) { this.gericht = gericht; }
    public Verein getVerein() { return verein; }
    public void setVerein(Verein verein) { this.verein = verein; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }
    public String getAktenzeichen() { return aktenzeichen; }
    public void setAktenzeichen(String aktenzeichen) { this.aktenzeichen = aktenzeichen; }
    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }
    public LocalDate getZieldatum() { return zieldatum; }
    public void setZieldatum(LocalDate zieldatum) { this.zieldatum = zieldatum; }
    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }
    public boolean isBezahlt() { return bezahlt; }
    public void setBezahlt(boolean bezahlt) { this.bezahlt = bezahlt; }
    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
    public List<Eingang> getEingaenge() { return eingaenge; }
}
