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
 * Bußgeld-Zuweisung durch ein {@link Gericht} an einen {@link Verein}.
 * Hält den geschuldeten Betrag sowie alle zugehörigen
 * {@link Eingang Zahlungseingänge}.
 *
 * @author Ole
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

    private LocalDate zieldatum;

    @Column(nullable = false)
    private BigDecimal betrag;

    @Column(nullable = false)
    private boolean bezahlt;

    private String bemerkung;

    @NotAudited
    @OneToMany(mappedBy = "bussgeld", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Eingang> eingaenge = new ArrayList<>();

    /** Liefert die ID des Bußgelds. */
    public Long getId() { return id; }

    /** Liefert das zuweisende Gericht. */
    public Gericht getGericht() { return gericht; }

    /** Setzt das zuweisende Gericht. */
    public void setGericht(Gericht gericht) { this.gericht = gericht; }

    /** Liefert den begünstigten Verein. */
    public Verein getVerein() { return verein; }

    /** Setzt den begünstigten Verein. */
    public void setVerein(Verein verein) { this.verein = verein; }

    /** Liefert den Bearbeitungsstatus. */
    public String getStatus() { return status; }

    /** Setzt den Bearbeitungsstatus. */
    public void setStatus(String status) { this.status = status; }

    /** Liefert den Nachnamen der zahlungspflichtigen Person. */
    public String getName() { return name; }

    /** Setzt den Nachnamen der zahlungspflichtigen Person. */
    public void setName(String name) { this.name = name; }

    /** Liefert den Vornamen der zahlungspflichtigen Person. */
    public String getVorname() { return vorname; }

    /** Setzt den Vornamen der zahlungspflichtigen Person. */
    public void setVorname(String vorname) { this.vorname = vorname; }

    /** Liefert das Aktenzeichen. */
    public String getAktenzeichen() { return aktenzeichen; }

    /** Setzt das Aktenzeichen. */
    public void setAktenzeichen(String aktenzeichen) { this.aktenzeichen = aktenzeichen; }

    /** Liefert das Zuweisungsdatum. */
    public LocalDate getDatum() { return datum; }

    /** Setzt das Zuweisungsdatum. */
    public void setDatum(LocalDate datum) { this.datum = datum; }

    /** Liefert das Zahlungsziel. */
    public LocalDate getZieldatum() { return zieldatum; }

    /** Setzt das Zahlungsziel. */
    public void setZieldatum(LocalDate zieldatum) { this.zieldatum = zieldatum; }

    /** Liefert den geschuldeten Betrag. */
    public BigDecimal getBetrag() { return betrag; }

    /** Setzt den geschuldeten Betrag. */
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    /** Gibt an, ob das Bußgeld vollständig bezahlt ist. */
    public boolean isBezahlt() { return bezahlt; }

    /** Setzt das Kennzeichen, ob das Bußgeld vollständig bezahlt ist. */
    public void setBezahlt(boolean bezahlt) { this.bezahlt = bezahlt; }

    /** Liefert die Bemerkung. */
    public String getBemerkung() { return bemerkung; }

    /** Setzt die Bemerkung. */
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }

    /** Liefert die Zahlungseingänge zu diesem Bußgeld. */
    public List<Eingang> getEingaenge() { return eingaenge; }
}
