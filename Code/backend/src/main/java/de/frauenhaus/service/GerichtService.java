package de.frauenhaus.service;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.repository.GerichtRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Nils
 *     <p>Pflege der Gerichte (alt: generisches CInfoFrame auf frauenhaus.gericht).
 */
@Service
@Transactional
public class GerichtService {

  private final GerichtRepository gerichte;
  private final DokumentService dokumente;

  public GerichtService(GerichtRepository gerichte, DokumentService dokumente) {
    this.gerichte = gerichte;
    this.dokumente = dokumente;
  }

  @Transactional(readOnly = true)
  public List<Gericht> alle(String suche) {
    String suchbegriff = normalisiereSuche(suche);
    return suchbegriff == null ? gerichte.findAll() : gerichte.suchen(suchbegriff);
  }

  @Transactional(readOnly = true)
  public Gericht finden(Long id) {
    return gerichte
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "Gericht " + id + " nicht gefunden"));
  }

  public Gericht anlegen(String bezeichnung, String strasse, String plz, String ort) {
    Gericht gericht = new Gericht();
    gericht.setBezeichnung(bezeichnung);
    gericht.setStrasse(strasse);
    gericht.setPlz(plz);
    gericht.setOrt(ort);
    return gerichte.save(gericht);
  }

  public Gericht aendern(Long id, String bezeichnung, String strasse, String plz, String ort) {
    Gericht gericht = finden(id);
    gericht.setBezeichnung(bezeichnung);
    gericht.setStrasse(strasse);
    gericht.setPlz(plz);
    gericht.setOrt(ort);
    return gericht;
  }

  public void loeschen(Long id) {
    if (!gerichte.existsById(id)) {
      throw new ResponseStatusException(NOT_FOUND, "Gericht " + id + " nicht gefunden");
    }
    try {
      dokumente.loescheFuerEntity(Dokument.EntityTyp.GERICHT, Long.toString(id));
      gerichte.deleteById(id);
      gerichte.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          CONFLICT, "Gericht " + id + " wird noch von Bußgeldern verwendet");
    }
  }

  private static String normalisiereSuche(String suche) {
    if (suche == null) {
      return null;
    }
    String suchbegriff = suche.trim();
    return suchbegriff.isEmpty() ? null : suchbegriff;
  }
}
