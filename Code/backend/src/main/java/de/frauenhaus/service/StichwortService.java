package de.frauenhaus.service;

import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.repository.StichwortRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * @author Nils
 *
 * Stichwort-Pflege, portiert aus CReportStichwortZusammenstellen
 * und CReportStichworteZusammenfassen.
 */
@Service
public class StichwortService {

    private final StichwortRepository stichworte;

    public StichwortService(StichwortRepository stichworte) {
        this.stichworte = stichworte;
    }

    /**
     * @author Nils
     *
     * Neues Stichwort aus bestehenden zusammenstellen (alte bleiben erhalten).
     */
    @Transactional
    public int zusammenstellen(String neu, Collection<String> alte) {
        stichworte.findById(neu).orElseGet(() -> stichworte.save(new Stichwort(neu)));
        return stichworte.stichworteZuordnen(neu, alte);
    }

    /**
     * @author Nils
     *
     * Stichworte zu einem neuen zusammenfassen, alte Stichworte werden gelöscht.
     */
    @Transactional
    public int zusammenfassen(String neu, Collection<String> alte) {
        int zugeordnet = zusammenstellen(neu, alte);
        stichworte.zuordnungenLoeschen(alte);
        stichworte.stichworteLoeschen(alte);
        return zugeordnet;
    }
}
