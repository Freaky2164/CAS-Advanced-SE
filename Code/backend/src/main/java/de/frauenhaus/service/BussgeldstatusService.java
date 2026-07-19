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
 * Pflege der zulässigen Bußgeld-Status.
 *
 * @author Ole
 */
@Service
@Transactional
public class BussgeldstatusService {

    private final BussgeldstatusRepository stati;

    /**
     * Erzeugt den Service mit dem Bußgeldstatus-Repository.
     *
     * @param stati das Bußgeldstatus-Repository
     */
    public BussgeldstatusService(BussgeldstatusRepository stati) {
        this.stati = stati;
    }

    /**
     * Liefert alle Bußgeld-Status.
     *
     * @return die vorhandenen Statuswerte
     */
    @Transactional(readOnly = true)
    public List<Bussgeldstatus> alle() {
        return stati.findAll();
    }

    /**
     * Legt einen neuen Bußgeldstatus an.
     *
     * @param name die Bezeichnung des Status
     * @return der angelegte Status
     */
    public Bussgeldstatus anlegen(String name) {
        if (stati.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Status '" + name + "' existiert bereits");
        }
        return stati.save(new Bussgeldstatus(name));
    }

    /**
     * Löscht einen Bußgeldstatus; schlägt fehl, wenn er noch verwendet wird.
     *
     * @param name die Bezeichnung des Status
     */
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
