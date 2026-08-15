package de.frauenhaus.domain;

import jakarta.persistence.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mitglied bzw. Adresse mit Kontaktdaten sowie der Zuordnung zu
 * Verteiler-{@link Stichwort Stichworten} und {@link Verein Vereinen}.
 *
 * @author Ole
 */
@Entity
@Table(name = "mitglied", schema = "frauenhaus")
@Audited
@AuditTable(value = "mitglied_aud", schema = "frauenhaus")
public class Mitglied {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mitglied")
    private Long id;

    private String anrede;
    private String vorname;
    private String name;
    private String name2;
    private String name3;
    private String briefanrede;
    private String strasse;
    private String plz;
    private String ort;
    private String email;
    private String tel1;
    private String tel2;
    private String fax;

    @Column(nullable = false)
    private boolean foerderverein;

    @Column(nullable = false)
    private boolean frauenhaus;

    private String bemerkung;

    @NotAudited
    @ManyToMany
    @JoinTable(name = "stichwort_person", schema = "frauenhaus",
            joinColumns = @JoinColumn(name = "mitglied"),
            inverseJoinColumns = @JoinColumn(name = "stichwort"))
    private Set<Stichwort> stichworte = new LinkedHashSet<>();

    @NotAudited
    @ManyToMany
    @JoinTable(name = "verein_mitglied", schema = "frauenhaus",
            joinColumns = @JoinColumn(name = "mitglied"),
            inverseJoinColumns = @JoinColumn(name = "verein"))
    private Set<Verein> vereine = new LinkedHashSet<>();

    /** Liefert die ID des Mitglieds. */
    public Long getId() { return id; }

    /** Liefert die Anrede. */
    public String getAnrede() { return anrede; }

    /** Setzt die Anrede. */
    public void setAnrede(String anrede) { this.anrede = anrede; }

    /** Liefert den Vornamen. */
    public String getVorname() { return vorname; }

    /** Setzt den Vornamen. */
    public void setVorname(String vorname) { this.vorname = vorname; }

    /** Liefert den Namen. */
    public String getName() { return name; }

    /** Setzt den Namen. */
    public void setName(String name) { this.name = name; }

    /** Liefert den zweiten Namenszusatz. */
    public String getName2() { return name2; }

    /** Setzt den zweiten Namenszusatz. */
    public void setName2(String name2) { this.name2 = name2; }

    /** Liefert den dritten Namenszusatz. */
    public String getName3() { return name3; }

    /** Setzt den dritten Namenszusatz. */
    public void setName3(String name3) { this.name3 = name3; }

    /** Liefert die Briefanrede. */
    public String getBriefanrede() { return briefanrede; }

    /** Setzt die Briefanrede. */
    public void setBriefanrede(String briefanrede) { this.briefanrede = briefanrede; }

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

    /** Liefert die E-Mail-Adresse. */
    public String getEmail() { return email; }

    /** Setzt die E-Mail-Adresse. */
    public void setEmail(String email) { this.email = email; }

    /** Liefert die erste Telefonnummer. */
    public String getTel1() { return tel1; }

    /** Setzt die erste Telefonnummer. */
    public void setTel1(String tel1) { this.tel1 = tel1; }

    /** Liefert die zweite Telefonnummer. */
    public String getTel2() { return tel2; }

    /** Setzt die zweite Telefonnummer. */
    public void setTel2(String tel2) { this.tel2 = tel2; }

    /** Liefert die Faxnummer. */
    public String getFax() { return fax; }

    /** Setzt die Faxnummer. */
    public void setFax(String fax) { this.fax = fax; }

    /** Gibt an, ob das Mitglied dem Förderverein angehört. */
    public boolean isFoerderverein() { return foerderverein; }

    /** Setzt die Zugehörigkeit zum Förderverein. */
    public void setFoerderverein(boolean foerderverein) { this.foerderverein = foerderverein; }

    /** Gibt an, ob das Mitglied dem Frauenhaus-Verein angehört. */
    public boolean isFrauenhaus() { return frauenhaus; }

    /** Setzt die Zugehörigkeit zum Frauenhaus-Verein. */
    public void setFrauenhaus(boolean frauenhaus) { this.frauenhaus = frauenhaus; }

    /** Liefert die Bemerkung. */
    public String getBemerkung() { return bemerkung; }

    /** Setzt die Bemerkung. */
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }

    /** Liefert die zugeordneten Verteiler-Stichworte. */
    public Set<Stichwort> getStichworte() { return stichworte; }

    /** Liefert die zugeordneten Vereine. */
    public Set<Verein> getVereine() { return vereine; }
}
