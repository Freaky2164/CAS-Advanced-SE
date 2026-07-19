package de.frauenhaus.service;

import de.frauenhaus.domain.Anrede;
import de.frauenhaus.repository.AnredeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der zulässigen Anreden.
 *
 * @author Paul
 */
@Service
@Transactional
public class AnredeService {

    private final AnredeRepository anreden;

    /**
     * Erzeugt den Service mit dem Anrede-Repository.
     *
     * @param anreden das Anrede-Repository
     */
    public AnredeService(AnredeRepository anreden) {
        this.anreden = anreden;
    }

    /**
     * Liefert alle Anreden.
     *
     * @return die vorhandenen Anreden
     */
    @Transactional(readOnly = true)
    public List<Anrede> alle() {
        return anreden.findAll();
    }

    /**
     * Legt eine neue Anrede an.
     *
     * @param name die Bezeichnung der Anrede
     * @return die angelegte Anrede
     */
    public Anrede anlegen(String name) {
        if (anreden.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Anrede '" + name + "' existiert bereits");
        }
        return anreden.save(new Anrede(name));
    }

    /**
     * Löscht eine Anrede; schlägt fehl, wenn sie noch verwendet wird.
     *
     * @param name die Bezeichnung der Anrede
     */
    public void loeschen(String name) {
        if (!anreden.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Anrede '" + name + "' nicht gefunden");
        }
        try {
            anreden.deleteById(name);
            anreden.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Anrede '" + name + "' wird noch von Mitgliedern verwendet");
        }
    }
}
