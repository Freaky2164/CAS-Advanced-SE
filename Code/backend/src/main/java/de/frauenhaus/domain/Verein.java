package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

/**
 * @author Nils
 *
 * Träger: 'Frauenhaus' oder 'Förderverein' (alt: Spaltenwert verein).
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

    /** Für JPA. */
    protected Verein() { }

    public Verein(String name, String bezeichnung) {
        this.name = name;
        this.bezeichnung = bezeichnung;
    }

    public String getName() { return name; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
}
