package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeldstatus;
import de.frauenhaus.repository.BussgeldstatusRepository;
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
 * Pflege der zulässigen Bußgeld-Status (alt: generisches CInfoFrame auf frauenhaus.bussgeldstatus).
 */
@Service
@Transactional
public class BussgeldstatusService {

    private final BussgeldstatusRepository stati;

    public BussgeldstatusService(BussgeldstatusRepository stati) {
        this.stati = stati;
    }

    @Transactional(readOnly = true)
    public List<Bussgeldstatus> alle() {
        return stati.findAll();
    }

    public Bussgeldstatus anlegen(String name) {
        if (stati.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Status '" + name + "' existiert bereits");
        }
        return stati.save(new Bussgeldstatus(name));
    }

    public void loeschen(String name) {
        if (!stati.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Status '" + name + "' nicht gefunden");
        }
        try {
            stati.deleteById(name);
            stati.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Status '" + name + "' wird noch von Bußgeldern verwendet");
        }
    }
}
