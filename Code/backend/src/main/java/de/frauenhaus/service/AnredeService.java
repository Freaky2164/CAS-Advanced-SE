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
 * @author Nils
 *
 * Pflege der zulässigen Anreden (alt: generisches CInfoFrame auf frauenhaus.anrede).
 */
@Service
@Transactional
public class AnredeService {

    private final AnredeRepository anreden;

    public AnredeService(AnredeRepository anreden) {
        this.anreden = anreden;
    }

    @Transactional(readOnly = true)
    public List<Anrede> alle() {
        return anreden.findAll();
    }

    public Anrede anlegen(String name) {
        if (anreden.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Anrede '" + name + "' existiert bereits");
        }
        return anreden.save(new Anrede(name));
    }

    public void loeschen(String name) {
        if (!anreden.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Anrede '" + name + "' nicht gefunden");
        }
        try {
            anreden.deleteById(name);
            anreden.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "Anrede '" + name + "' wird noch von Mitgliedern verwendet");
        }
    }
}
