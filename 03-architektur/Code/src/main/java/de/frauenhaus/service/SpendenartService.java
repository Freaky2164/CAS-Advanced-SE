package de.frauenhaus.service;

import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.repository.SpendenartRepository;
import de.frauenhaus.repository.SpendentypRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

/**
 * Pflege der Spendenarten.
 *
 * @author Nils
 */
@Service
@Transactional
public class SpendenartService {

    private final SpendenartRepository spendenarten;
    private final SpendentypRepository spendentypen;

    /**
     * Erzeugt den Service mit Spendenart- und Spendentyp-Repository.
     *
     * @param spendenarten das Spendenart-Repository
     * @param spendentypen das Spendentyp-Repository
     */
    public SpendenartService(SpendenartRepository spendenarten, SpendentypRepository spendentypen) {
        this.spendenarten = spendenarten;
        this.spendentypen = spendentypen;
    }

    /**
     * Liefert alle Spendenarten.
     *
     * @return die vorhandenen Spendenarten
     */
    @Transactional(readOnly = true)
    public List<Spendenart> alle() {
        return spendenarten.findAll();
    }

    /**
     * Lädt eine Spendenart oder wirft 404, wenn sie nicht existiert.
     *
     * @param name die Bezeichnung der Spendenart
     * @return die gefundene Spendenart
     */
    @Transactional(readOnly = true)
    public Spendenart finden(String name) {
        return holen(name);
    }

    /**
     * Legt eine neue Spendenart mit zugehörigem Spendentyp an.
     *
     * @param name die Bezeichnung der Spendenart
     * @param spendentyp die Bezeichnung des zugehörigen Spendentyps
     * @return die angelegte Spendenart
     */
    public Spendenart anlegen(String name, String spendentyp) {
        if (spendenarten.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Spendenart '" + name + "' existiert bereits");
        }
        pruefeSpendentyp(spendentyp);
        return spendenarten.save(new Spendenart(name, spendentyp));
    }

    /**
     * Ändert den Spendentyp einer Spendenart.
     *
     * @param name die Bezeichnung der Spendenart
     * @param spendentyp die Bezeichnung des neuen Spendentyps
     * @return die geänderte Spendenart
     */
    public Spendenart spendentypAendern(String name, String spendentyp) {
        pruefeSpendentyp(spendentyp);
        Spendenart spendenart = holen(name);
        spendenart.setSpendentyp(spendentyp);
        return spendenart;
    }

    /**
     * Löscht eine Spendenart; schlägt fehl, wenn sie noch verwendet wird.
     *
     * @param name die Bezeichnung der Spendenart
     */
    public void loeschen(String name) {
        if (!spendenarten.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Spendenart '" + name + "' nicht gefunden");
        }
        try {
            spendenarten.deleteById(name);
            spendenarten.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Spendenart '" + name + "' wird noch von Spenden verwendet");
        }
    }

    /**
     * Lädt eine Spendenart oder wirft 404, wenn sie nicht existiert.
     */
    private Spendenart holen(String name) {
        return spendenarten.findById(name)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Spendenart '" + name + "' nicht gefunden"));
    }

    /**
     * Prüft, ob der Spendentyp existiert, und wirft sonst 400.
     */
    private void pruefeSpendentyp(String spendentyp) {
        if (!spendentypen.existsById(spendentyp)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unbekannter Spendentyp '" + spendentyp + "'");
        }
    }
}
