package de.frauenhaus.service;

import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.repository.StichwortRepository;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nils
 *     <p>Stichwort-Pflege, portiert aus CReportStichwortZusammenstellen und
 *     CReportStichworteZusammenfassen.
 */
@Service
public class StichwortService {

  private final StichwortRepository stichworte;

  public StichwortService(StichwortRepository stichworte) {
    this.stichworte = stichworte;
  }

  /**
   * @author Nils
   *     <p>Neues Stichwort aus bestehenden zusammenstellen (alte bleiben erhalten).
   */
  @Transactional
  public int zusammenstellen(String neu, Collection<String> alte) {
    stichworte.findById(neu).orElseGet(() -> stichworte.save(new Stichwort(neu)));
    return stichworte.stichworteZuordnen(neu, alte);
  }

  /**
   * @author Nils
   *     <p>Stichworte zu einem neuen zusammenfassen, alte Stichworte werden gelöscht.
   */
  @Transactional
  public int zusammenfassen(String neu, Collection<String> alte) {
    int zugeordnet = zusammenstellen(neu, alte);
    stichworte.zuordnungenLoeschen(alte);
    stichworte.stichworteLoeschen(alte);
    return zugeordnet;
  }
}
