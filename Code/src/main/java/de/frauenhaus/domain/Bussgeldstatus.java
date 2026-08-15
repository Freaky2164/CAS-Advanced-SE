package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stammdaten-Entität für die zulässigen Statuswerte eines {@link Bussgeld}.
 *
 * @author Ole
 */
@Entity
@Table(name = "bussgeldstatus", schema = "frauenhaus")
public class Bussgeldstatus {

    @Id
    @Column(name = "bussgeldstatus")
    private String name;

    /** Parameterloser Konstruktor für JPA. */
    protected Bussgeldstatus() { }

    /**
     * Legt einen Bußgeldstatus mit dem angegebenen Namen an.
     *
     * @param name die Bezeichnung des Status
     */
    public Bussgeldstatus(String name) {
        this.name = name;
    }

    /** Liefert die Bezeichnung des Status. */
    public String getName() { return name; }
}
