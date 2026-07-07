package de.frauenhaus.domain;

import jakarta.persistence.*;

/**
 * @author Nils
 *
 * Gericht, das Bußgelder zuweist (alt: frauenhaus.gericht).
 */
@Entity
@Table(name = "gericht", schema = "frauenhaus")
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

    public Long getId() { return id; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getStrasse() { return strasse; }
    public void setStrasse(String strasse) { this.strasse = strasse; }
    public String getPlz() { return plz; }
    public void setPlz(String plz) { this.plz = plz; }
    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }
}
