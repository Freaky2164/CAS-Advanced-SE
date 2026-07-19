package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nils
 *     <p>Bußgeldstatus-Lookup für {@link Bussgeld} (alt: frauenhaus.bussgeldstatus). Bisher nur als
 *     DB-Fremdschlüssel-Constraint vorhanden, jetzt als eigenständige Stammdaten-Entität für die
 *     Pflege der zulässigen Status-Werte (Grundlage für Stammdaten-CRUD).
 */
@Entity
@Table(name = "bussgeldstatus", schema = "frauenhaus")
public class Bussgeldstatus {

  @Id
  @Column(name = "bussgeldstatus")
  private String name;

  /**
   * @author Nils
   *     <p>Für JPA.
   */
  protected Bussgeldstatus() {}

  public Bussgeldstatus(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
