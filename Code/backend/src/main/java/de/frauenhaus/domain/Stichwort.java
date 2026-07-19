package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Verteiler-Stichwort, mit dem {@link Mitglied}er für Serienbriefe und
 * E-Mail-Verteiler gruppiert werden.
 *
 * @author Nils
 */
@Entity
@Table(name = "stichwort", schema = "frauenhaus")
public class Stichwort {

    @Id
    @Column(name = "stichwort")
    private String name;

    /** Parameterloser Konstruktor für JPA. */
    protected Stichwort() { }

    /**
     * Legt ein Stichwort mit dem angegebenen Namen an.
     *
     * @param name die Bezeichnung des Stichworts
     */
    public Stichwort(String name) { this.name = name; }

    /** Liefert die Bezeichnung des Stichworts. */
    public String getName() { return name; }
}
