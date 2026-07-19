package de.frauenhaus.service;

import de.frauenhaus.domain.Spendentyp;
import de.frauenhaus.repository.SpendentypRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der zulässigen Spendentypen.
 *
 * @author Robin
 */
@Service
@Transactional
public class SpendentypService {

    private final SpendentypRepository spendentypen;

    /**
     * Erzeugt den Service mit dem Spendentyp-Repository.
     *
     * @param spendentypen das Spendentyp-Repository
     */
    public SpendentypService(SpendentypRepository spendentypen) {
        this.spendentypen = spendentypen;
    }

    /**
     * Liefert alle Spendentypen.
     *
     * @return die vorhandenen Spendentypen
     */
    @Transactional(readOnly = true)
    public List<Spendentyp> alle() {
        return spendentypen.findAll();
    }

    /**
     * Legt einen neuen Spendentyp an.
     *
     * @param name die Bezeichnung des Spendentyps
     * @return der angelegte Spendentyp
     */
    public Spendentyp anlegen(String name) {
        if (spendentypen.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Spendentyp '" + name + "' existiert bereits");
        }
        return spendentypen.save(new Spendentyp(name));
    }

    /**
     * Löscht einen Spendentyp; schlägt fehl, wenn er noch verwendet wird.
     *
     * @param name die Bezeichnung des Spendentyps
     */
    public void loeschen(String name) {
        if (!spendentypen.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Spendentyp '" + name + "' nicht gefunden");
        }
        try {
            spendentypen.deleteById(name);
            spendentypen.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Spendentyp '" + name + "' wird noch von Spendenarten verwendet");
        }
    }
}
