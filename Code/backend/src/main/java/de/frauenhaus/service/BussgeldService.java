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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * @author Nils
 *
 * Pflege der Bußgelder inkl. Zahlungseingänge (alt: generisches CInfoFrame auf
 * frauenhaus.bussgeld mit Sub-Tabelle frauenhaus.eingang, CInfoFrameStatusSub).
 */
@Service
@Transactional
public class BussgeldService {

    /**
     * @author Nils
     *
     * Zahlungseingang ohne Rückverweis auf das Bußgeld.
     */
    public record EingangResponse(Long id, LocalDate datum, BigDecimal betrag, String bemerkung) {
        static EingangResponse of(Eingang e) {
            return new EingangResponse(e.getId(), e.getDatum(), e.getBetrag(), e.getBemerkung());
        }
    }

    /**
     * @author Nils
     *
     * Bußgeld mit aufgelösten Zahlungseingängen statt lazy Collection.
     */
    public record BussgeldResponse(
            Long id, Long gerichtId, String gerichtBezeichnung, String verein, String status,
            String name, String vorname, String aktenzeichen, LocalDate datum, LocalDate zieldatum,
            BigDecimal betrag, boolean bezahlt, String bemerkung, List<EingangResponse> eingaenge) {

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

    public BussgeldService(BussgeldRepository bussgelder, GerichtRepository gerichte,
                            VereinRepository vereine, BussgeldstatusRepository stati,
                            DokumentService dokumente) {
        this.bussgelder = bussgelder;
        this.gerichte = gerichte;
        this.vereine = vereine;
        this.stati = stati;
        this.dokumente = dokumente;
    }

    @Transactional(readOnly = true)
    public Page<BussgeldResponse> alle(Pageable pageable, String suche) {
        String suchbegriff = normalisiereSuche(suche);
        Page<Bussgeld> seite = suchbegriff == null
                ? bussgelder.findAll(pageable)
                : bussgelder.suchen(suchbegriff, pageable);
        return seite.map(BussgeldResponse::of);
    }

    @Transactional(readOnly = true)
    public BussgeldResponse finden(Long id) {
        return BussgeldResponse.of(holen(id));
    }

    public BussgeldResponse anlegen(Long gerichtId, String verein, String status, String name, String vorname,
                                     String aktenzeichen, LocalDate datum, LocalDate zieldatum,
                                     BigDecimal betrag, boolean bezahlt, String bemerkung) {
        Bussgeld b = new Bussgeld();
        uebertragen(b, gerichtId, verein, status, name, vorname, aktenzeichen, datum, zieldatum, betrag, bezahlt, bemerkung);
        return BussgeldResponse.of(bussgelder.save(b));
    }

    public BussgeldResponse aendern(Long id, Long gerichtId, String verein, String status, String name, String vorname,
                                     String aktenzeichen, LocalDate datum, LocalDate zieldatum,
                                     BigDecimal betrag, boolean bezahlt, String bemerkung) {
        Bussgeld b = holen(id);
        uebertragen(b, gerichtId, verein, status, name, vorname, aktenzeichen, datum, zieldatum, betrag, bezahlt, bemerkung);
        return BussgeldResponse.of(b);
    }

    public void loeschen(Long id) {
        if (!bussgelder.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Bußgeld " + id + " nicht gefunden");
        }
        dokumente.loescheFuerEntity(Dokument.EntityTyp.BUSSGELD, Long.toString(id));
        bussgelder.deleteById(id);
    }

    /**
     * @author Nils
     *
     * Fügt einen Zahlungseingang hinzu (alt: CInfoFrameStatusSub -> neue Zeile in frauenhaus.eingang).
     */
    public BussgeldResponse eingangHinzufuegen(Long bussgeldId, LocalDate datum, BigDecimal betrag, String bemerkung) {
        Bussgeld b = holen(bussgeldId);
        Eingang e = new Eingang();
        e.setBussgeld(b);
        e.setDatum(datum);
        e.setBetrag(betrag);
        e.setBemerkung(bemerkung);
        b.getEingaenge().add(e);
        // Flush erzwingen, damit die IDENTITY-generierte ID des neuen Eingangs
        // schon in der Response verfügbar ist (nicht erst nach Transaktionsende).
        return BussgeldResponse.of(bussgelder.saveAndFlush(b));
    }

    /**
     * @author Nils
     *
     * Entfernt einen Zahlungseingang (orphanRemoval löscht ihn beim Speichern).
     */
    public BussgeldResponse eingangEntfernen(Long bussgeldId, Long eingangId) {
        Bussgeld b = holen(bussgeldId);
        boolean entfernt = b.getEingaenge().removeIf(e -> e.getId().equals(eingangId));
        if (!entfernt) {
            throw new ResponseStatusException(NOT_FOUND, "Zahlungseingang " + eingangId + " nicht gefunden");
        }
        return BussgeldResponse.of(b);
    }

    private Bussgeld holen(Long id) {
        return bussgelder.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Bußgeld " + id + " nicht gefunden"));
    }

    private static String normalisiereSuche(String suche) {
        if (suche == null) {
            return null;
        }
        String suchbegriff = suche.trim();
        return suchbegriff.isEmpty() ? null : suchbegriff;
    }

    private void uebertragen(Bussgeld b, Long gerichtId, String verein, String status, String name, String vorname,
                              String aktenzeichen, LocalDate datum, LocalDate zieldatum,
                              BigDecimal betrag, boolean bezahlt, String bemerkung) {
        Gericht gericht = gerichte.findById(gerichtId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekanntes Gericht " + gerichtId));
        Verein v = vereine.findById(verein)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unbekannter Verein '" + verein + "'"));
        if (status != null && !status.isBlank() && !stati.existsById(status)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unbekannter Status '" + status + "'");
        }
        b.setGericht(gericht);
        b.setVerein(v);
        b.setStatus(status);
        b.setName(name);
        b.setVorname(vorname);
        b.setAktenzeichen(aktenzeichen);
        b.setDatum(datum);
        b.setZieldatum(zieldatum);
        b.setBetrag(betrag);
        b.setBezahlt(bezahlt);
        b.setBemerkung(bemerkung);
    }
}
