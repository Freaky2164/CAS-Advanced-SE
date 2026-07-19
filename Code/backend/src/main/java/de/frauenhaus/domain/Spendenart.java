package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stammdaten-Entität für Spendenarten; jede Spendenart ist einem
 * {@link Spendentyp} zugeordnet.
 *
 * @author Paul
 */
@Entity
@Table(name = "spendenart", schema = "frauenhaus")
public class Spendenart {

    @Id
    @Column(name = "spendenart")
    private String name;

    @Column(nullable = false)
    private String spendentyp;

    /** Parameterloser Konstruktor für JPA. */
    protected Spendenart() { }

    /**
     * Legt eine Spendenart mit Name und zugehörigem Spendentyp an.
     *
     * @param name die Bezeichnung der Spendenart
     * @param spendentyp die Bezeichnung des zugehörigen Spendentyps
     */
    public Spendenart(String name, String spendentyp) {
        this.name = name;
        this.spendentyp = spendentyp;
    }

    /** Liefert die Bezeichnung der Spendenart. */
    public String getName() { return name; }

    /** Liefert die Bezeichnung des zugehörigen Spendentyps. */
    public String getSpendentyp() { return spendentyp; }

    /** Setzt die Bezeichnung des zugehörigen Spendentyps. */
    public void setSpendentyp(String spendentyp) { this.spendentyp = spendentyp; }
}
