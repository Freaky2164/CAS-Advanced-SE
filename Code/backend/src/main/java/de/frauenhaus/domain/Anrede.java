package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nils
 *     <p>Anrede-Lookup für {@link Mitglied} (alt: frauenhaus.anrede). Bisher nur als
 *     DB-Fremdschlüssel-Constraint vorhanden, jetzt als eigenständige Stammdaten-Entität für die
 *     Pflege der zulässigen Anreden (Grundlage für Stammdaten-CRUD).
 */
@Entity
@Table(name = "anrede", schema = "frauenhaus")
public class Anrede {

  @Id
  @Column(name = "anrede")
  private String name;

  /**
   * @author Nils
   *     <p>Für JPA.
   */
  protected Anrede() {}

  public Anrede(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
