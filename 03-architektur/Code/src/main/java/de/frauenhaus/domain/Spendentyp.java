package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stammdaten-Entität für die zulässigen Spendentypen einer {@link Spendenart}.
 *
 * @author Ole
 */
@Entity
@Table(name = "spendentyp", schema = "frauenhaus")
public class Spendentyp {

    @Id
    @Column(name = "spendentyp")
    private String name;

    /** Parameterloser Konstruktor für JPA. */
    protected Spendentyp() { }

    /**
     * Legt einen Spendentyp mit dem angegebenen Namen an.
     *
     * @param name die Bezeichnung des Spendentyps
     */
    public Spendentyp(String name) {
        this.name = name;
    }

    /** Liefert die Bezeichnung des Spendentyps. */
    public String getName() { return name; }
}
