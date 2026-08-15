package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

/**
 * Gericht, das Bußgelder zuweist.
 *
 * @author Ole
 */
@Entity
@Table(name = "gericht", schema = "frauenhaus")
@Audited
@AuditTable(value = "gericht_aud", schema = "frauenhaus")
public class Gericht {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gericht")
    private Long id;

    @Column(nullable = false)
    private String bezeichnung;

    private String strasse;
    private String plz;
    private String ort;

    /** Liefert die ID des Gerichts. */
    public Long getId() { return id; }

    /** Liefert die Bezeichnung des Gerichts. */
    public String getBezeichnung() { return bezeichnung; }

    /** Setzt die Bezeichnung des Gerichts. */
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }

    /** Liefert die Straße der Anschrift. */
    public String getStrasse() { return strasse; }

    /** Setzt die Straße der Anschrift. */
    public void setStrasse(String strasse) { this.strasse = strasse; }

    /** Liefert die Postleitzahl der Anschrift. */
    public String getPlz() { return plz; }

    /** Setzt die Postleitzahl der Anschrift. */
    public void setPlz(String plz) { this.plz = plz; }

    /** Liefert den Ort der Anschrift. */
    public String getOrt() { return ort; }

    /** Setzt den Ort der Anschrift. */
    public void setOrt(String ort) { this.ort = ort; }
}
