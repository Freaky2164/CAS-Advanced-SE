package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nils
 *
 * Spendentyp-Lookup für {@link Spendenart} (alt: frauenhaus.spendentyp), z.B.
 * 'Geldspende dauer', 'Mitgliedsbeitrag', 'Dauerspende', 'Sachspende'. Bisher nur als
 * DB-Fremdschlüssel-Constraint vorhanden, jetzt als eigenständige Stammdaten-Entität
 * für die Pflege der zulässigen Spendentypen (Grundlage für Stammdaten-CRUD).
 */
@Entity
@Table(name = "spendentyp", schema = "frauenhaus")
public class Spendentyp {

    @Id
    @Column(name = "spendentyp")
    private String name;

    /** Für JPA. */
    protected Spendentyp() { }

    public Spendentyp(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
