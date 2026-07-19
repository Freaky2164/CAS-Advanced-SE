package de.frauenhaus.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.AnredeRepository;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.StichwortRepository;
import de.frauenhaus.repository.VereinRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Nils
 *     <p>Pflege der Mitglieder/Adressen (alt: generisches CInfoFrame auf frauenhaus.mitglied, inkl.
 *     Verteiler- und Vereinszuordnung). Antworten werden als {@link MitgliedResponse} ausgeliefert,
 *     damit die lazy geladenen Stichwort-/Verein-Zuordnungen innerhalb der Transaktion aufgelöst
 *     werden (kein Open-Session-in-View).
 */
@Service
@Transactional
public class MitgliedService {

  /**
   * @author Nils
   *     <p>Vollständige Mitglied-Daten inkl. aufgelöster Stichwort-/Vereinsnamen.
   */
  public record MitgliedResponse(
      Long id,
      String anrede,
      String vorname,
      String name,
      String name2,
      String name3,
      String briefanrede,
      String strasse,
      String plz,
      String ort,
      String email,
      String tel1,
      String tel2,
      String fax,
      boolean foerderverein,
      boolean frauenhaus,
      String bemerkung,
      List<String> stichworte,
      List<String> vereine) {

    static MitgliedResponse of(Mitglied m) {
      return new MitgliedResponse(
          m.getId(),
          m.getAnrede(),
          m.getVorname(),
          m.getName(),
          m.getName2(),
          m.getName3(),
          m.getBriefanrede(),
          m.getStrasse(),
          m.getPlz(),
          m.getOrt(),
          m.getEmail(),
          m.getTel1(),
          m.getTel2(),
          m.getFax(),
          m.isFoerderverein(),
          m.isFrauenhaus(),
          m.getBemerkung(),
          m.getStichworte().stream().map(Stichwort::getName).sorted().toList(),
          m.getVereine().stream().map(Verein::getName).sorted().toList());
    }
  }

  private final MitgliedRepository mitglieder;
  private final AnredeRepository anreden;
  private final StichwortRepository stichworte;
  private final VereinRepository vereine;
  private final DokumentService dokumente;

  public MitgliedService(
      MitgliedRepository mitglieder,
      AnredeRepository anreden,
      StichwortRepository stichworte,
      VereinRepository vereine,
      DokumentService dokumente) {
    this.mitglieder = mitglieder;
    this.anreden = anreden;
    this.stichworte = stichworte;
    this.vereine = vereine;
    this.dokumente = dokumente;
  }

  @Transactional(readOnly = true)
  public Page<MitgliedResponse> alle(Pageable pageable, String suche) {
    String suchbegriff = normalisiereSuche(suche);
    Page<Mitglied> seite =
        suchbegriff == null
            ? mitglieder.findAll(pageable)
            : mitglieder.suchen(suchbegriff, pageable);
    return seite.map(MitgliedResponse::of);
  }

  @Transactional(readOnly = true)
  public MitgliedResponse finden(Long id) {
    return MitgliedResponse.of(holen(id));
  }

  public MitgliedResponse anlegen(
      Mitglied vorlage, List<String> stichwortNamen, List<String> vereinNamen) {
    pruefeAnrede(vorlage.getAnrede());
    Mitglied m = new Mitglied();
    uebertragen(vorlage, m);
    zuordnungenSetzen(m, stichwortNamen, vereinNamen);
    return MitgliedResponse.of(mitglieder.save(m));
  }

  public MitgliedResponse aendern(
      Long id, Mitglied vorlage, List<String> stichwortNamen, List<String> vereinNamen) {
    pruefeAnrede(vorlage.getAnrede());
    Mitglied m = holen(id);
    uebertragen(vorlage, m);
    zuordnungenSetzen(m, stichwortNamen, vereinNamen);
    return MitgliedResponse.of(m);
  }

  /**
   * @author Nils
   *     <p>Dupliziert ein Mitglied inkl. Stammdaten und Zuordnungen (alt: CInfoFrameStatusCopy).
   */
  public MitgliedResponse duplizieren(Long id) {
    Mitglied original = holen(id);
    Mitglied kopie = new Mitglied();
    uebertragen(original, kopie);
    kopie.getStichworte().addAll(original.getStichworte());
    kopie.getVereine().addAll(original.getVereine());
    return MitgliedResponse.of(mitglieder.save(kopie));
  }

  public void loeschen(Long id) {
    if (!mitglieder.existsById(id)) {
      throw new ResponseStatusException(NOT_FOUND, "Mitglied " + id + " nicht gefunden");
    }
    try {
      dokumente.loescheFuerEntity(Dokument.EntityTyp.MITGLIED, Long.toString(id));
      mitglieder.deleteById(id);
      mitglieder.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          CONFLICT, "Mitglied " + id + " wird noch von Spenden verwendet");
    }
  }

  private Mitglied holen(Long id) {
    return mitglieder
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "Mitglied " + id + " nicht gefunden"));
  }

  private void pruefeAnrede(String anrede) {
    if (anrede != null && !anrede.isBlank() && !anreden.existsById(anrede)) {
      throw new ResponseStatusException(BAD_REQUEST, "Unbekannte Anrede '" + anrede + "'");
    }
  }

  private static String normalisiereSuche(String suche) {
    if (suche == null) {
      return null;
    }
    String suchbegriff = suche.trim();
    return suchbegriff.isEmpty() ? null : suchbegriff;
  }

  private static void uebertragen(Mitglied quelle, Mitglied ziel) {
    ziel.setAnrede(quelle.getAnrede());
    ziel.setVorname(quelle.getVorname());
    ziel.setName(quelle.getName());
    ziel.setName2(quelle.getName2());
    ziel.setName3(quelle.getName3());
    ziel.setBriefanrede(quelle.getBriefanrede());
    ziel.setStrasse(quelle.getStrasse());
    ziel.setPlz(quelle.getPlz());
    ziel.setOrt(quelle.getOrt());
    ziel.setEmail(quelle.getEmail());
    ziel.setTel1(quelle.getTel1());
    ziel.setTel2(quelle.getTel2());
    ziel.setFax(quelle.getFax());
    ziel.setFoerderverein(quelle.isFoerderverein());
    ziel.setFrauenhaus(quelle.isFrauenhaus());
    ziel.setBemerkung(quelle.getBemerkung());
  }

  private void zuordnungenSetzen(
      Mitglied m, List<String> stichwortNamen, List<String> vereinNamen) {
    if (stichwortNamen != null) {
      Set<Stichwort> gefunden = new LinkedHashSet<>(stichworte.findAllById(stichwortNamen));
      if (gefunden.size() != new LinkedHashSet<>(stichwortNamen).size()) {
        throw new ResponseStatusException(
            BAD_REQUEST, "Unbekanntes Stichwort in " + stichwortNamen);
      }
      m.getStichworte().clear();
      m.getStichworte().addAll(gefunden);
    }
    if (vereinNamen != null) {
      Set<Verein> gefunden = new LinkedHashSet<>(vereine.findAllById(vereinNamen));
      if (gefunden.size() != new LinkedHashSet<>(vereinNamen).size()) {
        throw new ResponseStatusException(BAD_REQUEST, "Unbekannter Verein in " + vereinNamen);
      }
      m.getVereine().clear();
      m.getVereine().addAll(gefunden);
    }
  }
}
