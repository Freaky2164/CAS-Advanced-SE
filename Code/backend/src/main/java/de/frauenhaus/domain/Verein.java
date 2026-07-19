package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

/**
 * Trägerverein, dem Spenden und Bußgelder zugeordnet werden.
 *
 * @author Robin
 */
@Entity
@Table(name = "verein", schema = "frauenhaus")
@Audited
@AuditTable(value = "verein_aud", schema = "frauenhaus")
public class Verein {

    @Id
    @Column(name = "verein")
    private String name;

    private String bezeichnung;

    /** Parameterloser Konstruktor für JPA. */
    protected Verein() { }

    /**
     * Legt einen Verein mit Kurzname und Bezeichnung an.
     *
     * @param name der Kurzname des Vereins
     * @param bezeichnung die ausgeschriebene Bezeichnung
     */
    public Verein(String name, String bezeichnung) {
        this.name = name;
        this.bezeichnung = bezeichnung;
    }

    /** Liefert den Kurznamen des Vereins. */
    public String getName() { return name; }

    /** Liefert die ausgeschriebene Bezeichnung. */
    public String getBezeichnung() { return bezeichnung; }

    /** Setzt die ausgeschriebene Bezeichnung. */
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
}
