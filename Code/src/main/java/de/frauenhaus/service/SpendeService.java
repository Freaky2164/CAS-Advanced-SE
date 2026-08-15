package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import de.frauenhaus.repository.SpendenartRepository;
import de.frauenhaus.repository.VereinRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der Spenden.
 *
 * @author Nils
 */
@Service
@Transactional
public class SpendeService {

    /**
     * Spende mit aufgelöstem Mitgliedsnamen statt verschachtelter Entity.
     */
    public record SpendeResponse(Long id, Long mitgliedId, String mitgliedName, String spendenart,
                                  String verein, LocalDate datum, BigDecimal betrag, String bemerkung)
            implements Serializable {
        /** Bildet eine {@link Spende} auf die Antwortdarstellung ab. */
        static SpendeResponse of(Spende s) {
            Mitglied m = s.getMitglied();
            String mitgliedName = (m.getVorname() != null ? m.getVorname() + " " : "") + m.getName();
            return new SpendeResponse(s.getId(), m.getId(), mitgliedName, s.getSpendenart().getName(),
                    s.getVerein().getName(), s.getDatum(), s.getBetrag(), s.getBemerkung());
        }
    }

    private final SpendeRepository spenden;
    private final MitgliedRepository mitglieder;
    private final SpendenartRepository spendenarten;
    private final VereinRepository vereine;
    private final DokumentService dokumente;

    /**
     * Erzeugt den Service mit den benötigten Repositories und dem
     * Dokument-Service.
     *
     * @param spenden das Spenden-Repository
     * @param mitglieder das Mitglieder-Repository
     * @param spendenarten das Spendenart-Repository
     * @param vereine das Verein-Repository
     * @param dokumente der Dokument-Service für angehängte Dokumente
     */
    public SpendeService(SpendeRepository spenden, MitgliedRepository mitglieder,
                          SpendenartRepository spendenarten, VereinRepository vereine,
                          DokumentService dokumente) {
        this.spenden = spenden;
        this.mitglieder = mitglieder;
        this.spendenarten = spendenarten;
        this.vereine = vereine;
        this.dokumente = dokumente;
    }

    /**
     * Liefert die Spenden seitenweise, optional gefiltert über einen
     * Suchbegriff.
     *
     * @param pageable die gewünschte Seite und Sortierung
     * @param suche der Suchbegriff oder {@code null} für alle
     * @return die passenden Spenden seitenweise
     */
    @Transactional(readOnly = true)
    public Page<SpendeResponse> alle(Pageable pageable, String suche) {
        String suchbegriff = normalisiereSuche(suche);
        Page<Spende> seite = suchbegriff == null
                ? spenden.findAll(pageable)
                : spenden.suchen(suchbegriff, pageable);
        return seite.map(SpendeResponse::of);
    }

    /**
     * Lädt eine Spende oder wirft 404, wenn sie nicht existiert.
     *
     * @param id die ID der Spende
     * @return die gefundene Spende
     */
    @Transactional(readOnly = true)
    public SpendeResponse finden(Long id) {
        return SpendeResponse.of(holen(id));
    }

    /**
     * Legt eine neue Spende an.
     *
     * @param mitgliedId die ID des spendenden Mitglieds
     * @param spendenart die Bezeichnung der Spendenart
     * @param verein der Kurzname des begünstigten Vereins
     * @param datum das Spendendatum
     * @param betrag der gespendete Betrag
     * @param bemerkung eine optionale Bemerkung
     * @return die angelegte Spende
     */
    public SpendeResponse anlegen(Long mitgliedId, String spendenart, String verein,
                                   LocalDate datum, BigDecimal betrag, String bemerkung) {
        Spende s = new Spende();
        uebertragen(s, mitgliedId, spendenart, verein, datum, betrag, bemerkung);
        return SpendeResponse.of(spenden.save(s));
    }

    /**
     * Ändert eine bestehende Spende.
     *
     * @param id die ID der Spende
     * @param mitgliedId die ID des spendenden Mitglieds
     * @param spendenart die Bezeichnung der Spendenart
     * @param verein der Kurzname des begünstigten Vereins
     * @param datum das Spendendatum
     * @param betrag der gespendete Betrag
     * @param bemerkung eine optionale Bemerkung
     * @return die geänderte Spende
     */
    public SpendeResponse aendern(Long id, Long mitgliedId, String spendenart, String verein,
                                   LocalDate datum, BigDecimal betrag, String bemerkung) {
        Spende s = holen(id);
        uebertragen(s, mitgliedId, spendenart, verein, datum, betrag, bemerkung);
        return SpendeResponse.of(s);
    }

    /**
     * Löscht eine Spende samt angehängter Dokumente.
     *
     * @param id die ID der Spende
     */
    public void loeschen(Long id) {
        if (!spenden.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Spende " + id + " nicht gefunden");
        }
        dokumente.loescheFuerEntity(Dokument.EntityTyp.SPENDE, Long.toString(id));
        spenden.deleteById(id);
    }

    /**
     * Lädt eine Spende oder wirft 404, wenn sie nicht existiert.
     */
    private Spende holen(Long id) {
        return spenden.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Spende " + id + " nicht gefunden"));
    }

    /**
     * Trimmt den Suchbegriff und liefert {@code null}, wenn er leer ist.
     */
    private static String normalisiereSuche(String suche) {
        if (suche == null) {
            return null;
        }
        String suchbegriff = suche.trim();
        return suchbegriff.isEmpty() ? null : suchbegriff;
    }

    /**
     * Überträgt die Eingabewerte auf die Spende; unbekannte Referenzen führen
     * zu 400.
     */
    private void uebertragen(Spende s, Long mitgliedId, String spendenart, String verein,
                              LocalDate datum, BigDecimal betrag, String bemerkung) {
        Mitglied m = mitglieder.findById(mitgliedId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekanntes Mitglied " + mitgliedId));
        Spendenart art = spendenarten.findById(spendenart)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekannte Spendenart '" + spendenart + "'"));
        Verein v = vereine.findById(verein)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekannter Verein '" + verein + "'"));
        s.setMitglied(m);
        s.setSpendenart(art);
        s.setVerein(v);
        s.setDatum(datum);
        s.setBetrag(betrag);
        s.setBemerkung(bemerkung);
    }
}
