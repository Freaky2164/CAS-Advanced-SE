package de.frauenhaus.service;

import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.repository.StichwortRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Pflege der Verteiler-Stichworte: Zusammenstellen und Zusammenfassen.
 *
 * @author Paul
 */
@Service
public class StichwortService {

    private final StichwortRepository stichworte;

    /**
     * Erzeugt den Service mit dem Stichwort-Repository.
     *
     * @param stichworte das Stichwort-Repository
     */
    public StichwortService(StichwortRepository stichworte) {
        this.stichworte = stichworte;
    }

    /**
     * Stellt ein neues Stichwort aus bestehenden zusammen; die alten
     * Stichworte bleiben erhalten.
     *
     * @param neu der Name des neuen Stichworts
     * @param alte die Namen der bestehenden Stichworte
     * @return die Anzahl der neu zugeordneten Mitglieder
     */
    @Transactional
    public int zusammenstellen(String neu, Collection<String> alte) {
        return zuordnen(neu, alte);
    }

    /**
     * Fasst Stichworte zu einem neuen zusammen; die alten Stichworte werden
     * gelöscht.
     *
     * @param neu der Name des neuen Stichworts
     * @param alte die Namen der zusammenzufassenden Stichworte
     * @return die Anzahl der neu zugeordneten Mitglieder
     */
    @Transactional
    public int zusammenfassen(String neu, Collection<String> alte) {
        int zugeordnet = zuordnen(neu, alte);
        stichworte.zuordnungenLoeschen(alte);
        stichworte.stichworteLoeschen(alte);
        return zugeordnet;
    }

    /**
     * Legt das Zielstichwort bei Bedarf an und ordnet ihm die Mitglieder der
     * bestehenden Stichworte zu.
     */
    private int zuordnen(String neu, Collection<String> alte) {
        if (stichworte.findById(neu).isEmpty()) {
            stichworte.save(new Stichwort(neu));
        }
        return stichworte.stichworteZuordnen(neu, alte);
    }
}
