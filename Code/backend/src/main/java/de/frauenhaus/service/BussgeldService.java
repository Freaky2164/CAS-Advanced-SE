package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Dokument;
import de.frauenhaus.domain.Eingang;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.BussgeldstatusRepository;
import de.frauenhaus.repository.GerichtRepository;
import de.frauenhaus.repository.VereinRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Pflege der Bußgelder inklusive Zahlungseingänge.
 *
 * @author Paul
 */
@Service
@Transactional
public class BussgeldService {

    /**
     * Eingabedaten eines Bußgelds für Anlegen und Ändern.
     *
     * @param gerichtId die ID des zuweisenden Gerichts
     * @param verein der Kurzname des begünstigten Vereins
     * @param status der Bearbeitungsstatus
     * @param name der Nachname der zahlungspflichtigen Person
     * @param vorname der Vorname der zahlungspflichtigen Person
     * @param aktenzeichen das Aktenzeichen
     * @param datum das Zuweisungsdatum
     * @param zieldatum das Zahlungsziel
     * @param betrag der geschuldete Betrag
     * @param bezahlt ob das Bußgeld vollständig bezahlt ist
     * @param bemerkung eine optionale Bemerkung
     */
    public record BussgeldDaten(Long gerichtId, String verein, String status, String name, String vorname,
                                String aktenzeichen, LocalDate datum, LocalDate zieldatum,
                                BigDecimal betrag, boolean bezahlt, String bemerkung) implements Serializable { }

    /**
     * Zahlungseingang ohne Rückverweis auf das Bußgeld.
     */
    public record EingangResponse(Long id, LocalDate datum, BigDecimal betrag, String bemerkung)
            implements Serializable {
        /** Bildet einen {@link Eingang} auf die Antwortdarstellung ab. */
        static EingangResponse of(Eingang e) {
            return new EingangResponse(e.getId(), e.getDatum(), e.getBetrag(), e.getBemerkung());
        }
    }

    /**
     * Bußgeld mit aufgelösten Zahlungseingängen statt lazy Collection.
     */
    public record BussgeldResponse(
            Long id, Long gerichtId, String gerichtBezeichnung, String verein, String status,
            String name, String vorname, String aktenzeichen, LocalDate datum, LocalDate zieldatum,
            BigDecimal betrag, boolean bezahlt, String bemerkung, List<EingangResponse> eingaenge)
            implements Serializable {

        /** Bildet ein {@link Bussgeld} auf die Antwortdarstellung ab. */
        static BussgeldResponse of(Bussgeld b) {
            return new BussgeldResponse(
                    b.getId(), b.getGericht().getId(), b.getGericht().getBezeichnung(), b.getVerein().getName(),
                    b.getStatus(), b.getName(), b.getVorname(), b.getAktenzeichen(), b.getDatum(), b.getZieldatum(),
                    b.getBetrag(), b.isBezahlt(), b.getBemerkung(),
                    b.getEingaenge().stream().map(EingangResponse::of).toList());
        }
    }

    private final BussgeldRepository bussgelder;
    private final GerichtRepository gerichte;
    private final VereinRepository vereine;
    private final BussgeldstatusRepository stati;
    private final DokumentService dokumente;

    /**
     * Erzeugt den Service mit den benötigten Repositories und dem
     * Dokument-Service.
     *
     * @param bussgelder das Bußgeld-Repository
     * @param gerichte das Gericht-Repository
     * @param vereine das Verein-Repository
     * @param stati das Bußgeldstatus-Repository
     * @param dokumente der Dokument-Service für angehängte Dokumente
     */
    public BussgeldService(BussgeldRepository bussgelder, GerichtRepository gerichte,
                            VereinRepository vereine, BussgeldstatusRepository stati,
                            DokumentService dokumente) {
        this.bussgelder = bussgelder;
        this.gerichte = gerichte;
        this.vereine = vereine;
        this.stati = stati;
        this.dokumente = dokumente;
    }

    /**
     * Liefert die Bußgelder seitenweise, optional gefiltert über einen
     * Suchbegriff.
     *
     * @param pageable die gewünschte Seite und Sortierung
     * @param suche der Suchbegriff oder {@code null} für alle
     * @return die passenden Bußgelder seitenweise
     */
    @Transactional(readOnly = true)
    public Page<BussgeldResponse> alle(Pageable pageable, String suche) {
        String suchbegriff = normalisiereSuche(suche);
        Page<Bussgeld> seite = suchbegriff == null
                ? bussgelder.findAll(pageable)
                : bussgelder.suchen(suchbegriff, pageable);
        return seite.map(BussgeldResponse::of);
    }

    /**
     * Lädt ein Bußgeld oder wirft 404, wenn es nicht existiert.
     *
     * @param id die ID des Bußgelds
     * @return das gefundene Bußgeld
     */
    @Transactional(readOnly = true)
    public BussgeldResponse finden(Long id) {
        return BussgeldResponse.of(holen(id));
    }

    /**
     * Legt ein neues Bußgeld an.
     *
     * @param daten die Eingabedaten des Bußgelds
     * @return das angelegte Bußgeld
     */
    public BussgeldResponse anlegen(BussgeldDaten daten) {
        Bussgeld b = new Bussgeld();
        uebertragen(b, daten);
        return BussgeldResponse.of(bussgelder.save(b));
    }

    /**
     * Ändert ein bestehendes Bußgeld.
     *
     * @param id die ID des Bußgelds
     * @param daten die neuen Eingabedaten
     * @return das geänderte Bußgeld
     */
    public BussgeldResponse aendern(Long id, BussgeldDaten daten) {
        Bussgeld b = holen(id);
        uebertragen(b, daten);
        return BussgeldResponse.of(b);
    }

    /**
     * Löscht ein Bußgeld samt angehängter Dokumente.
     *
     * @param id die ID des Bußgelds
     */
    public void loeschen(Long id) {
        if (!bussgelder.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Bußgeld " + id + " nicht gefunden");
        }
        dokumente.loescheFuerEntity(Dokument.EntityTyp.BUSSGELD, Long.toString(id));
        bussgelder.deleteById(id);
    }

    /**
     * Fügt dem Bußgeld einen Zahlungseingang hinzu. Speichert mit Flush, damit
     * die generierte ID des neuen Eingangs bereits in der Antwort verfügbar ist.
     *
     * @param bussgeldId die ID des Bußgelds
     * @param datum das Eingangsdatum
     * @param betrag der eingegangene Betrag
     * @param bemerkung eine optionale Bemerkung
     * @return das Bußgeld mit dem neuen Zahlungseingang
     */
    public BussgeldResponse eingangHinzufuegen(Long bussgeldId, LocalDate datum, BigDecimal betrag, String bemerkung) {
        Bussgeld b = holen(bussgeldId);
        Eingang e = new Eingang();
        e.setBussgeld(b);
        e.setDatum(datum);
        e.setBetrag(betrag);
        e.setBemerkung(bemerkung);
        b.getEingaenge().add(e);
        return BussgeldResponse.of(bussgelder.saveAndFlush(b));
    }

    /**
     * Entfernt einen Zahlungseingang; orphanRemoval löscht ihn beim Speichern.
     *
     * @param bussgeldId die ID des Bußgelds
     * @param eingangId die ID des Zahlungseingangs
     * @return das Bußgeld ohne den entfernten Zahlungseingang
     */
    public BussgeldResponse eingangEntfernen(Long bussgeldId, Long eingangId) {
        Bussgeld b = holen(bussgeldId);
        boolean entfernt = b.getEingaenge().removeIf(e -> e.getId().equals(eingangId));
        if (!entfernt) {
            throw new ResponseStatusException(NOT_FOUND, "Zahlungseingang " + eingangId + " nicht gefunden");
        }
        return BussgeldResponse.of(b);
    }

    /**
     * Lädt ein Bußgeld oder wirft 404, wenn es nicht existiert.
     */
    private Bussgeld holen(Long id) {
        return bussgelder.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bußgeld " + id + " nicht gefunden"));
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
     * Überträgt die Eingabewerte auf das Bußgeld; unbekannte Referenzen führen
     * zu 400.
     */
    private void uebertragen(Bussgeld b, BussgeldDaten daten) {
        Gericht gericht = gerichte.findById(daten.gerichtId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekanntes Gericht " + daten.gerichtId()));
        Verein v = vereine.findById(daten.verein())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekannter Verein '" + daten.verein() + "'"));
        if (daten.status() != null && !daten.status().isBlank() && !stati.existsById(daten.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "Unbekannter Status '" + daten.status() + "'");
        }
        b.setGericht(gericht);
        b.setVerein(v);
        b.setStatus(daten.status());
        b.setName(daten.name());
        b.setVorname(daten.vorname());
        b.setAktenzeichen(daten.aktenzeichen());
        b.setDatum(daten.datum());
        b.setZieldatum(daten.zieldatum());
        b.setBetrag(daten.betrag());
        b.setBezahlt(daten.bezahlt());
        b.setBemerkung(daten.bemerkung());
    }
}
