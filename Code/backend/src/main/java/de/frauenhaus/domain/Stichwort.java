package de.frauenhaus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Nils
 *     <p>Verteiler-Stichwort, mit dem {@link Mitglied}er für Serienbriefe/E-Mail-Verteiler
 *     gruppiert werden (alt: frauenhaus.stichwort).
 */
@Entity
@Table(name = "stichwort", schema = "frauenhaus")
public class Stichwort {

  @Id
  @Column(name = "stichwort")
  private String name;

  /**
   * @author Nils
   *     <p>Für JPA.
   */
  protected Stichwort() {}

  public Stichwort(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
