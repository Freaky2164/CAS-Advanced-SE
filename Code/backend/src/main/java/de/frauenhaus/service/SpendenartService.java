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
 * @author Nils
 *
 * Pflege der Spendenarten (alt: generisches CInfoFrame auf frauenhaus.spendenart).
 */
@Service
@Transactional
public class SpendenartService {

    private final SpendenartRepository spendenarten;
    private final SpendentypRepository spendentypen;

    public SpendenartService(SpendenartRepository spendenarten, SpendentypRepository spendentypen) {
        this.spendenarten = spendenarten;
        this.spendentypen = spendentypen;
    }

    @Transactional(readOnly = true)
    public List<Spendenart> alle() {
        return spendenarten.findAll();
    }

    @Transactional(readOnly = true)
    public Spendenart finden(String name) {
        return spendenarten.findById(name)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Spendenart '" + name + "' nicht gefunden"));
    }

    public Spendenart anlegen(String name, String spendentyp) {
        if (spendenarten.existsById(name)) {
            throw new ResponseStatusException(CONFLICT, "Spendenart '" + name + "' existiert bereits");
        }
        pruefeSpendentyp(spendentyp);
        return spendenarten.save(new Spendenart(name, spendentyp));
    }

    public Spendenart spendentypAendern(String name, String spendentyp) {
        pruefeSpendentyp(spendentyp);
        Spendenart spendenart = finden(name);
        spendenart.setSpendentyp(spendentyp);
        return spendenart;
    }

    public void loeschen(String name) {
        if (!spendenarten.existsById(name)) {
            throw new ResponseStatusException(NOT_FOUND, "Spendenart '" + name + "' nicht gefunden");
        }
        try {
            spendenarten.deleteById(name);
            spendenarten.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(CONFLICT, "Spendenart '" + name + "' wird noch von Spenden verwendet");
        }
    }

    private void pruefeSpendentyp(String spendentyp) {
        if (!spendentypen.existsById(spendentyp)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unbekannter Spendentyp '" + spendentyp + "'");
        }
    }
}
