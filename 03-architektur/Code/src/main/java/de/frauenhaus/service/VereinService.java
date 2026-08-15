package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.VereinRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der Trägervereine.
 *
 * @author Nils
 */
@Service
@Transactional
public class VereinService {

    private final VereinRepository vereine;
    private final DokumentService dokumente;

    /**
     * Erzeugt den Service mit Verein-Repository und Dokument-Service.
     *
     * @param vereine das Verein-Repository
     * @param dokumente der Dokument-Service für angehängte Dokumente
     */
    public VereinService(VereinRepository vereine, DokumentService dokumente) {
        this.vereine = vereine;
        this.dokumente = dokumente;
    }

    /**
     * Liefert alle Vereine, optional gefiltert über einen Suchbegriff.
     *
     * @param suche der Suchbegriff oder {@code null} für alle
     * @return die passenden Vereine
     */
    @Transactional(readOnly = true)
    public List<Verein> alle(String suche) {
        String suchbegriff = normalisiereSuche(suche);
        return suchbegriff == null ? vereine.findAll() : vereine.suchen(suchbegriff);
    }

    /**
     * Lädt einen Verein oder wirft 404, wenn er nicht existiert.
     *
     * @param name der Kurzname des Vereins
     * @return der gefundene Verein
     */
    @Transactional(readOnly = true)
    public Verein finden(String name) {
        return holen(name);
    }

    /**
     * Legt einen neuen Verein an.
     *
     * @param name der Kurzname des Vereins
     * @param bezeichnung die ausgeschriebene Bezeichnung
     * @return der angelegte Verein
     */
    public Verein anlegen(String name, String bezeichnung) {
        if (vereine.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Verein '" + name + "' existiert bereits");
        }
        return vereine.save(new Verein(name, bezeichnung));
    }

    /**
     * Ändert die Bezeichnung eines Vereins.
     *
     * @param name der Kurzname des Vereins
     * @param bezeichnung die neue Bezeichnung
     * @return der geänderte Verein
     */
    public Verein bezeichnungAendern(String name, String bezeichnung) {
        Verein verein = holen(name);
        verein.setBezeichnung(bezeichnung);
        return verein;
    }

    /**
     * Löscht einen Verein samt angehängter Dokumente; schlägt fehl, wenn er
     * noch verwendet wird.
     *
     * @param name der Kurzname des Vereins
     */
    public void loeschen(String name) {
        if (!vereine.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Verein '" + name + "' nicht gefunden");
        }
        try {
            dokumente.loescheFuerEntity(Dokument.EntityTyp.VEREIN, name);
            vereine.deleteById(name);
            vereine.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Verein '" + name + "' wird noch verwendet");
        }
    }

    /**
     * Lädt einen Verein oder wirft 404, wenn er nicht existiert.
     */
    private Verein holen(String name) {
        return vereine.findById(name)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verein '" + name + "' nicht gefunden"));
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
