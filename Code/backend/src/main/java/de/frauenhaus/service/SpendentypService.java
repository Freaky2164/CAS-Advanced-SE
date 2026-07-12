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
 * @author Nils
 *
 * Pflege der zulässigen Spendentypen (alt: generisches CInfoFrame auf frauenhaus.spendentyp).
 */
@Service
@Transactional
public class SpendentypService {

    private final SpendentypRepository spendentypen;

    public SpendentypService(SpendentypRepository spendentypen) {
        this.spendentypen = spendentypen;
    }

    @Transactional(readOnly = true)
    public List<Spendentyp> alle() {
        return spendentypen.findAll();
    }

    public Spendentyp anlegen(String name) {
        if (spendentypen.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Spendentyp '" + name + "' existiert bereits");
        }
        return spendentypen.save(new Spendentyp(name));
    }

    public void loeschen(String name) {
        if (!spendentypen.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Spendentyp '" + name + "' nicht gefunden");
        }
        try {
            spendentypen.deleteById(name);
            spendentypen.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "Spendentyp '" + name + "' wird noch von Spendenarten verwendet");
        }
    }
}
