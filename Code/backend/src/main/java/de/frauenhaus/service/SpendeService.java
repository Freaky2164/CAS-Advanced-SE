package de.frauenhaus.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import de.frauenhaus.repository.SpendenartRepository;
import de.frauenhaus.repository.VereinRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Nils
 *     <p>Pflege der Spenden (alt: generisches CInfoFrame auf frauenhaus.spende).
 */
@Service
@Transactional
public class SpendeService {

  /**
   * @author Nils
   *     <p>Spende mit aufgelöstem Mitgliedsnamen statt verschachtelter Entity.
   */
  public record SpendeResponse(
      Long id,
      Long mitgliedId,
      String mitgliedName,
      String spendenart,
      String verein,
      LocalDate datum,
      BigDecimal betrag,
      String bemerkung) {
    static SpendeResponse of(Spende s) {
      Mitglied m = s.getMitglied();
      String mitgliedName = (m.getVorname() != null ? m.getVorname() + " " : "") + m.getName();
      return new SpendeResponse(
          s.getId(),
          m.getId(),
          mitgliedName,
          s.getSpendenart().getName(),
          s.getVerein().getName(),
          s.getDatum(),
          s.getBetrag(),
          s.getBemerkung());
    }
  }

  private final SpendeRepository spenden;
  private final MitgliedRepository mitglieder;
  private final SpendenartRepository spendenarten;
  private final VereinRepository vereine;
  private final DokumentService dokumente;

  public SpendeService(
      SpendeRepository spenden,
      MitgliedRepository mitglieder,
      SpendenartRepository spendenarten,
      VereinRepository vereine,
      DokumentService dokumente) {
    this.spenden = spenden;
    this.mitglieder = mitglieder;
    this.spendenarten = spendenarten;
    this.vereine = vereine;
    this.dokumente = dokumente;
  }

  @Transactional(readOnly = true)
  public Page<SpendeResponse> alle(Pageable pageable, String suche) {
    String suchbegriff = normalisiereSuche(suche);
    Page<Spende> seite =
        suchbegriff == null ? spenden.findAll(pageable) : spenden.suchen(suchbegriff, pageable);
    return seite.map(SpendeResponse::of);
  }

  @Transactional(readOnly = true)
  public SpendeResponse finden(Long id) {
    return SpendeResponse.of(holen(id));
  }

  public SpendeResponse anlegen(
      Long mitgliedId,
      String spendenart,
      String verein,
      LocalDate datum,
      BigDecimal betrag,
      String bemerkung) {
    Spende s = new Spende();
    uebertragen(s, mitgliedId, spendenart, verein, datum, betrag, bemerkung);
    return SpendeResponse.of(spenden.save(s));
  }

  public SpendeResponse aendern(
      Long id,
      Long mitgliedId,
      String spendenart,
      String verein,
      LocalDate datum,
      BigDecimal betrag,
      String bemerkung) {
    Spende s = holen(id);
    uebertragen(s, mitgliedId, spendenart, verein, datum, betrag, bemerkung);
    return SpendeResponse.of(s);
  }

  public void loeschen(Long id) {
    if (!spenden.existsById(id)) {
      throw new ResponseStatusException(NOT_FOUND, "Spende " + id + " nicht gefunden");
    }
    dokumente.loescheFuerEntity(Dokument.EntityTyp.SPENDE, Long.toString(id));
    spenden.deleteById(id);
  }

  private Spende holen(Long id) {
    return spenden
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "Spende " + id + " nicht gefunden"));
  }

  private static String normalisiereSuche(String suche) {
    if (suche == null) {
      return null;
    }
    String suchbegriff = suche.trim();
    return suchbegriff.isEmpty() ? null : suchbegriff;
  }

  private void uebertragen(
      Spende s,
      Long mitgliedId,
      String spendenart,
      String verein,
      LocalDate datum,
      BigDecimal betrag,
      String bemerkung) {
    Mitglied m =
        mitglieder
            .findById(mitgliedId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(BAD_REQUEST, "Unbekanntes Mitglied " + mitgliedId));
    Spendenart art =
        spendenarten
            .findById(spendenart)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        BAD_REQUEST, "Unbekannte Spendenart '" + spendenart + "'"));
    Verein v =
        vereine
            .findById(verein)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        BAD_REQUEST, "Unbekannter Verein '" + verein + "'"));
    s.setMitglied(m);
    s.setSpendenart(art);
    s.setVerein(v);
    s.setDatum(datum);
    s.setBetrag(betrag);
    s.setBemerkung(bemerkung);
  }
}
