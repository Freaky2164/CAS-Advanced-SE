package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.repository.GerichtRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der Gerichte.
 *
 * @author Nils
 */
@Service
@Transactional
public class GerichtService {

    private final GerichtRepository gerichte;
    private final DokumentService dokumente;

    /**
     * Erzeugt den Service mit Gericht-Repository und Dokument-Service.
     *
     * @param gerichte das Gericht-Repository
     * @param dokumente der Dokument-Service für angehängte Dokumente
     */
    public GerichtService(GerichtRepository gerichte, DokumentService dokumente) {
        this.gerichte = gerichte;
        this.dokumente = dokumente;
    }

    /**
     * Liefert alle Gerichte, optional gefiltert über einen Suchbegriff.
     *
     * @param suche der Suchbegriff oder {@code null} für alle
     * @return die passenden Gerichte
     */
    @Transactional(readOnly = true)
    public List<Gericht> alle(String suche) {
        String suchbegriff = normalisiereSuche(suche);
        return suchbegriff == null ? gerichte.findAll() : gerichte.suchen(suchbegriff);
    }

    /**
     * Lädt ein Gericht oder wirft 404, wenn es nicht existiert.
     *
     * @param id die ID des Gerichts
     * @return das gefundene Gericht
     */
    @Transactional(readOnly = true)
    public Gericht finden(Long id) {
        return holen(id);
    }

    /**
     * Legt ein neues Gericht an.
     *
     * @param bezeichnung die Bezeichnung des Gerichts
     * @param strasse die Straße der Anschrift
     * @param plz die Postleitzahl der Anschrift
     * @param ort der Ort der Anschrift
     * @return das angelegte Gericht
     */
    public Gericht anlegen(String bezeichnung, String strasse, String plz, String ort) {
        Gericht gericht = new Gericht();
        gericht.setBezeichnung(bezeichnung);
        gericht.setStrasse(strasse);
        gericht.setPlz(plz);
        gericht.setOrt(ort);
        return gerichte.save(gericht);
    }

    /**
     * Ändert Bezeichnung und Anschrift eines Gerichts.
     *
     * @param id die ID des Gerichts
     * @param bezeichnung die neue Bezeichnung
     * @param strasse die neue Straße
     * @param plz die neue Postleitzahl
     * @param ort der neue Ort
     * @return das geänderte Gericht
     */
    public Gericht aendern(Long id, String bezeichnung, String strasse, String plz, String ort) {
        Gericht gericht = holen(id);
        gericht.setBezeichnung(bezeichnung);
        gericht.setStrasse(strasse);
        gericht.setPlz(plz);
        gericht.setOrt(ort);
        return gericht;
    }

    /**
     * Löscht ein Gericht samt angehängter Dokumente; schlägt fehl, wenn es
     * noch von Bußgeldern verwendet wird.
     *
     * @param id die ID des Gerichts
     */
    public void loeschen(Long id) {
        if (!gerichte.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Gericht " + id + " nicht gefunden");
        }
        try {
            dokumente.loescheFuerEntity(Dokument.EntityTyp.GERICHT, Long.toString(id));
            gerichte.deleteById(id);
            gerichte.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Gericht " + id + " wird noch von Bußgeldern verwendet");
        }
    }

    /**
     * Lädt ein Gericht oder wirft 404, wenn es nicht existiert.
     */
    private Gericht holen(Long id) {
        return gerichte.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Gericht " + id + " nicht gefunden"));
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
}
