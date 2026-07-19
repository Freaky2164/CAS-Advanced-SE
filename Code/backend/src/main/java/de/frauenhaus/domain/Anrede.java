package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stammdaten-Entität für die zulässigen Anreden eines {@link Mitglied}.
 *
 * @author Paul
 */
@Entity
@Table(name = "anrede", schema = "frauenhaus")
public class Anrede {

    @Id
    @Column(name = "anrede")
    private String name;

    /** Parameterloser Konstruktor für JPA. */
    protected Anrede() { }

    /**
     * Legt eine Anrede mit dem angegebenen Namen an.
     *
     * @param name die Bezeichnung der Anrede
     */
    public Anrede(String name) {
        this.name = name;
    }

    /** Liefert die Bezeichnung der Anrede. */
    public String getName() { return name; }
}
