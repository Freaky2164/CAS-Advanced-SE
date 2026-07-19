package de.frauenhaus.service;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.VereinRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Nils
 *     <p>Pflege der Träger (alt: generisches CInfoFrame auf frauenhaus.verein; im Altsystem i.d.R.
 *     nur 'Frauenhaus' und 'Förderverein').
 */
@Service
@Transactional
public class VereinService {

  private final VereinRepository vereine;
  private final DokumentService dokumente;

  public VereinService(VereinRepository vereine, DokumentService dokumente) {
    this.vereine = vereine;
    this.dokumente = dokumente;
  }

  @Transactional(readOnly = true)
  public List<Verein> alle(String suche) {
    String suchbegriff = normalisiereSuche(suche);
    return suchbegriff == null ? vereine.findAll() : vereine.suchen(suchbegriff);
  }

  @Transactional(readOnly = true)
  public Verein finden(String name) {
    return vereine
        .findById(name)
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "Verein '" + name + "' nicht gefunden"));
  }

  public Verein anlegen(String name, String bezeichnung) {
    if (vereine.existsById(name)) {
      throw new ResponseStatusException(CONFLICT, "Verein '" + name + "' existiert bereits");
    }
    return vereine.save(new Verein(name, bezeichnung));
  }

  public Verein bezeichnungAendern(String name, String bezeichnung) {
    Verein verein = finden(name);
    verein.setBezeichnung(bezeichnung);
    return verein;
  }

  public void loeschen(String name) {
    if (!vereine.existsById(name)) {
      throw new ResponseStatusException(NOT_FOUND, "Verein '" + name + "' nicht gefunden");
    }
    try {
      dokumente.loescheFuerEntity(Dokument.EntityTyp.VEREIN, name);
      vereine.deleteById(name);
      vereine.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(CONFLICT, "Verein '" + name + "' wird noch verwendet");
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
