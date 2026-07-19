package de.frauenhaus.service;

import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.AnredeRepository;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.StichwortRepository;
import de.frauenhaus.repository.VereinRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der Mitglieder und Adressen. Antworten werden als
 * {@link MitgliedResponse} ausgeliefert, damit die lazy geladenen
 * Stichwort- und Verein-Zuordnungen innerhalb der Transaktion aufgelöst
 * werden.
 *
 * @author Robin
 */
@Service
@Transactional
public class MitgliedService {

    /**
     * Vollständige Mitglied-Daten inklusive aufgelöster Stichwort- und
     * Vereinsnamen.
     */
    public record MitgliedResponse(
            Long id, String anrede, String vorname, String name, String name2, String name3,
            String briefanrede, String strasse, String plz, String ort, String email,
            String tel1, String tel2, String fax, boolean foerderverein, boolean frauenhaus,
            String bemerkung, List<String> stichworte, List<String> vereine) implements Serializable {

        /** Bildet ein {@link Mitglied} auf die Antwortdarstellung ab. */
        static MitgliedResponse of(Mitglied m) {
            return new MitgliedResponse(
                    m.getId(), m.getAnrede(), m.getVorname(), m.getName(), m.getName2(), m.getName3(),
                    m.getBriefanrede(), m.getStrasse(), m.getPlz(), m.getOrt(), m.getEmail(),
                    m.getTel1(), m.getTel2(), m.getFax(), m.isFoerderverein(), m.isFrauenhaus(),
                    m.getBemerkung(),
                    m.getStichworte().stream().map(Stichwort::getName).sorted().toList(),
                    m.getVereine().stream().map(Verein::getName).sorted().toList());
        }
    }

    private final MitgliedRepository mitglieder;
    private final AnredeRepository anreden;
    private final StichwortRepository stichworte;
    private final VereinRepository vereine;
    private final DokumentService dokumente;

    /**
     * Erzeugt den Service mit den benötigten Repositories und dem
     * Dokument-Service.
     *
     * @param mitglieder das Mitglieder-Repository
     * @param anreden das Anrede-Repository
     * @param stichworte das Stichwort-Repository
     * @param vereine das Verein-Repository
     * @param dokumente der Dokument-Service für angehängte Dokumente
     */
    public MitgliedService(MitgliedRepository mitglieder, AnredeRepository anreden,
                            StichwortRepository stichworte, VereinRepository vereine,
                            DokumentService dokumente) {
        this.mitglieder = mitglieder;
        this.anreden = anreden;
        this.stichworte = stichworte;
        this.vereine = vereine;
        this.dokumente = dokumente;
    }

    /**
     * Liefert die Mitglieder seitenweise, optional gefiltert über einen
     * Suchbegriff.
     *
     * @param pageable die gewünschte Seite und Sortierung
     * @param suche der Suchbegriff oder {@code null} für alle
     * @return die passenden Mitglieder seitenweise
     */
    @Transactional(readOnly = true)
    public Page<MitgliedResponse> alle(Pageable pageable, String suche) {
        String suchbegriff = normalisiereSuche(suche);
        Page<Mitglied> seite = suchbegriff == null
                ? mitglieder.findAll(pageable)
                : mitglieder.suchen(suchbegriff, pageable);
        return seite.map(MitgliedResponse::of);
    }

    /**
     * Lädt ein Mitglied oder wirft 404, wenn es nicht existiert.
     *
     * @param id die ID des Mitglieds
     * @return das gefundene Mitglied
     */
    @Transactional(readOnly = true)
    public MitgliedResponse finden(Long id) {
        return MitgliedResponse.of(holen(id));
    }

    /**
     * Legt ein neues Mitglied an.
     *
     * @param vorlage die Stammdaten des neuen Mitglieds
     * @param stichwortNamen die zuzuordnenden Stichworte ({@code null} lässt sie leer)
     * @param vereinNamen die zuzuordnenden Vereine ({@code null} lässt sie leer)
     * @return das angelegte Mitglied
     */
    public MitgliedResponse anlegen(Mitglied vorlage, List<String> stichwortNamen, List<String> vereinNamen) {
        pruefeAnrede(vorlage.getAnrede());
        Mitglied m = new Mitglied();
        uebertragen(vorlage, m);
        zuordnungenSetzen(m, stichwortNamen, vereinNamen);
        return MitgliedResponse.of(mitglieder.save(m));
    }

    /**
     * Ändert ein bestehendes Mitglied.
     *
     * @param id die ID des Mitglieds
     * @param vorlage die neuen Stammdaten
     * @param stichwortNamen die zuzuordnenden Stichworte ({@code null} lässt sie unverändert)
     * @param vereinNamen die zuzuordnenden Vereine ({@code null} lässt sie unverändert)
     * @return das geänderte Mitglied
     */
    public MitgliedResponse aendern(Long id, Mitglied vorlage, List<String> stichwortNamen, List<String> vereinNamen) {
        pruefeAnrede(vorlage.getAnrede());
        Mitglied m = holen(id);
        uebertragen(vorlage, m);
        zuordnungenSetzen(m, stichwortNamen, vereinNamen);
        return MitgliedResponse.of(m);
    }

    /**
     * Dupliziert ein Mitglied inklusive Stammdaten und Zuordnungen.
     *
     * @param id die ID des zu duplizierenden Mitglieds
     * @return die angelegte Kopie
     */
    public MitgliedResponse duplizieren(Long id) {
        Mitglied original = holen(id);
        Mitglied kopie = new Mitglied();
        uebertragen(original, kopie);
        kopie.getStichworte().addAll(original.getStichworte());
        kopie.getVereine().addAll(original.getVereine());
        return MitgliedResponse.of(mitglieder.save(kopie));
    }

    /**
     * Löscht ein Mitglied samt angehängter Dokumente; schlägt fehl, wenn es
     * noch von Spenden verwendet wird.
     *
     * @param id die ID des Mitglieds
     */
    public void loeschen(Long id) {
        if (!mitglieder.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Mitglied " + id + " nicht gefunden");
        }
        try {
            dokumente.loescheFuerEntity(Dokument.EntityTyp.MITGLIED, Long.toString(id));
            mitglieder.deleteById(id);
            mitglieder.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ResponseStatusException(CONFLICT, "Mitglied " + id + " wird noch von Spenden verwendet");
        }
    }

    /**
     * Lädt ein Mitglied oder wirft 404, wenn es nicht existiert.
     */
    private Mitglied holen(Long id) {
        return mitglieder.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Mitglied " + id + " nicht gefunden"));
    }

    /**
     * Prüft, ob die Anrede existiert, und wirft sonst 400.
     */
    private void pruefeAnrede(String anrede) {
        if (anrede != null && !anrede.isBlank() && !anreden.existsById(anrede)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unbekannte Anrede '" + anrede + "'");
        }
    }

    /**
     * Trimmt den Suchbegriff und liefert {@code null}, wenn er leer ist.
     */
    private static String normalisiereSuche(String suche) {
        if (suche == null) {
            return null;
        }
        String suchbegriff = suche.trim();
        return suchbegriff.isEmpty() ? null : suchbegriff;
    }

    /**
     * Überträgt die Stammdaten von der Quelle auf das Ziel-Mitglied.
     */
    private static void uebertragen(Mitglied quelle, Mitglied ziel) {
        ziel.setAnrede(quelle.getAnrede());
        ziel.setVorname(quelle.getVorname());
        ziel.setName(quelle.getName());
        ziel.setName2(quelle.getName2());
        ziel.setName3(quelle.getName3());
        ziel.setBriefanrede(quelle.getBriefanrede());
        ziel.setStrasse(quelle.getStrasse());
        ziel.setPlz(quelle.getPlz());
        ziel.setOrt(quelle.getOrt());
        ziel.setEmail(quelle.getEmail());
        ziel.setTel1(quelle.getTel1());
        ziel.setTel2(quelle.getTel2());
        ziel.setFax(quelle.getFax());
        ziel.setFoerderverein(quelle.isFoerderverein());
        ziel.setFrauenhaus(quelle.isFrauenhaus());
        ziel.setBemerkung(quelle.getBemerkung());
    }

    /**
     * Ersetzt die Stichwort- und Verein-Zuordnungen; unbekannte Namen führen
     * zu 400, {@code null} lässt die jeweilige Zuordnung unverändert.
     */
    private void zuordnungenSetzen(Mitglied m, List<String> stichwortNamen, List<String> vereinNamen) {
        if (stichwortNamen != null) {
            Set<Stichwort> gefunden = new LinkedHashSet<>(stichworte.findAllById(stichwortNamen));
            if (gefunden.size() != new LinkedHashSet<>(stichwortNamen).size()) {
                throw new ResponseStatusException(BAD_REQUEST, "Unbekanntes Stichwort in " + stichwortNamen);
            }
            m.getStichworte().clear();
            m.getStichworte().addAll(gefunden);
        }
        if (vereinNamen != null) {
            Set<Verein> gefunden = new LinkedHashSet<>(vereine.findAllById(vereinNamen));
            if (gefunden.size() != new LinkedHashSet<>(vereinNamen).size()) {
                throw new ResponseStatusException(BAD_REQUEST, "Unbekannter Verein in " + vereinNamen);
            }
            m.getVereine().clear();
            m.getVereine().addAll(gefunden);
        }
    }
}
