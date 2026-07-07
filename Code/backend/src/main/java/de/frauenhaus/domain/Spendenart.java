package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nils
 *
 * Spendenart mit Typ, z.B. 'Geldspende dauer', 'Mitgliedsbeitrag' (alt: frauenhaus.spendenart).
 */
@Entity
@Table(name = "spendenart", schema = "frauenhaus")
public class Spendenart {

    @Id
    @Column(name = "spendenart")
    private String name;

    @Column(nullable = false)
    private String spendentyp;

    /** Für JPA. */
    protected Spendenart() { }

    public Spendenart(String name, String spendentyp) {
        this.name = name;
        this.spendentyp = spendentyp;
    }

    public String getName() { return name; }
    public String getSpendentyp() { return spendentyp; }
    public void setSpendentyp(String spendentyp) { this.spendentyp = spendentyp; }
}
