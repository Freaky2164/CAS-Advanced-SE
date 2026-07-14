package de.frauenhaus.domain;

import jakarta.persistence.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Nils
 *
 * Mitglieder/Adressen (alt: frauenhaus.mitglied). Trägt neben den Kontaktdaten
 * die Zuordnung zu Verteiler-{@link Stichwort Stichworten} und {@link Verein Vereinen}.
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

    /**
     * @author Nils
     *
     * Mitglied im Förderverein (Flag aus dem Altsystem).
     */
    @Column(nullable = false)
    private boolean foerderverein;

    /**
     * @author Nils
     *
     * Mitglied im Frauenhaus-Verein (Flag aus dem Altsystem).
     */
    @Column(nullable = false)
    private boolean frauenhaus;

    private String bemerkung;

    /**
     * @author Nils
     *
     * Verteiler-Zuordnung (alt: frauenhaus.stichwort_person).
     */
    @NotAudited
    @ManyToMany
    @JoinTable(name = "stichwort_person", schema = "frauenhaus",
            joinColumns = @JoinColumn(name = "mitglied"),
            inverseJoinColumns = @JoinColumn(name = "stichwort"))
    private Set<Stichwort> stichworte = new LinkedHashSet<>();

    /**
     * @author Nils
     *
     * Vereinszugehörigkeit (alt: frauenhaus.verein_mitglied).
     */
    @NotAudited
    @ManyToMany
    @JoinTable(name = "verein_mitglied", schema = "frauenhaus",
            joinColumns = @JoinColumn(name = "mitglied"),
            inverseJoinColumns = @JoinColumn(name = "verein"))
    private Set<Verein> vereine = new LinkedHashSet<>();

    public Long getId() { return id; }
    public String getAnrede() { return anrede; }
    public void setAnrede(String anrede) { this.anrede = anrede; }
    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getName2() { return name2; }
    public void setName2(String name2) { this.name2 = name2; }
    public String getName3() { return name3; }
    public void setName3(String name3) { this.name3 = name3; }
    public String getBriefanrede() { return briefanrede; }
    public void setBriefanrede(String briefanrede) { this.briefanrede = briefanrede; }
    public String getStrasse() { return strasse; }
    public void setStrasse(String strasse) { this.strasse = strasse; }
    public String getPlz() { return plz; }
    public void setPlz(String plz) { this.plz = plz; }
    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTel1() { return tel1; }
    public void setTel1(String tel1) { this.tel1 = tel1; }
    public String getTel2() { return tel2; }
    public void setTel2(String tel2) { this.tel2 = tel2; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public boolean isFoerderverein() { return foerderverein; }
    public void setFoerderverein(boolean foerderverein) { this.foerderverein = foerderverein; }
    public boolean isFrauenhaus() { return frauenhaus; }
    public void setFrauenhaus(boolean frauenhaus) { this.frauenhaus = frauenhaus; }
    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }
    public Set<Stichwort> getStichworte() { return stichworte; }
    public Set<Verein> getVereine() { return vereine; }
}
