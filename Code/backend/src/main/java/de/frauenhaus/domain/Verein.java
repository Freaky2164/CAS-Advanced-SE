package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nils
 *
 * Träger: 'Frauenhaus' oder 'Förderverein' (alt: Spaltenwert verein).
 */
@Entity
@Table(name = "verein", schema = "frauenhaus")
public class Verein {

    @Id
    @Column(name = "verein")
    private String name;

    private String bezeichnung;

    /** Für JPA. */
    protected Verein() { }

    public String getName() { return name; }
    public String getBezeichnung() { return bezeichnung; }
}
