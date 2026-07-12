package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Eingang;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.SpendeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nils
 *
 * Erzeugt Dokumente aus den Word-Vorlagen in {@code vorlagen/}
 * (alt: CCommandBestaetigungBussgeld und CCommandSpendenQuittung mit Word-COM):
 * Bußgeldbestätigungen aus FHBG.dot bzw. FVBG.dot, Spendenbescheinigungen aus
 * den FHSB- bzw. FVSB-Vorlagen je Träger und Spendentyp. Befüllt werden die
 * Lesezeichen der Vorlagen, das Ergebnis ist ein Word-Dokument (.doc).
 */
@Service
@Transactional(readOnly = true)
public class DocumentCreationService {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Vorlagen-Namenszusatz je Spendentyp (FHSB + Zusatz + ".dot"). */
    private static final Map<String, String> VORLAGE_JE_SPENDENTYP = Map.of(
            "Geldspende dauer", "Dauerspende",
            "Geldspende einmalig", "Geldspende",
            "Mitgliedsbeitrag", "Mitgliedsbeitrag",
            "Sachspende", "Sachspende");

    private final BussgeldRepository bussgelder;
    private final SpendeRepository spenden;
    private final Path vorlagen;

    public DocumentCreationService(BussgeldRepository bussgelder,
                                   SpendeRepository spenden,
                                   @Value("${app.vorlagen.pfad}") String vorlagenPfad) {
        this.bussgelder = bussgelder;
        this.spenden = spenden;
        this.vorlagen = Path.of(vorlagenPfad);
    }

    /**
     * Zahlungsbestätigung an das Gericht (Vorlage FHBG.dot bzw. FVBG.dot):
     * Anschrift des Gerichts, Strafsache, Bußgeldbetrag sowie die Liste der
     * Zahlungseingänge mit Restsummen-Hinweis.
     */
    public byte[] bussgeldBestaetigung(Long bussgeldId) {
        Bussgeld b = bussgelder.findById(bussgeldId)
                .orElseThrow(() -> new IllegalArgumentException("Bußgeld " + bussgeldId + " nicht gefunden"));
        NumberFormat waehrung = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        List<Eingang> eingaenge = b.getEingaenge().stream()
                .sorted(Comparator.comparing(Eingang::getDatum))
                .toList();
        BigDecimal gezahlt = eingaenge.stream()
                .map(Eingang::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, String> werte = new HashMap<>();
        werte.put("bezeichnung", b.getGericht().getBezeichnung());
        werte.put("strasse", b.getGericht().getStrasse());
        werte.put("plz", b.getGericht().getPlz());
        werte.put("ort", b.getGericht().getOrt());
        werte.put("name", b.getName());
        werte.put("vorname", b.getVorname());
        werte.put("aktenzeichen", b.getAktenzeichen() == null ? "unbekannt" : b.getAktenzeichen());
        werte.put("betrag", waehrung.format(b.getBetrag()));
        werte.put("datum", DATUM.format(LocalDate.now(ZoneId.systemDefault())));
        werte.put("datumbetrag", eingaenge.stream()
                .map(e -> DATUM.format(e.getDatum()) + "\t" + waehrung.format(e.getBetrag()))
                .collect(Collectors.joining("\r")));
        werte.put("restsumme", restsummenText(b.getBetrag(), gezahlt, waehrung));

        return DocumentCreationHelpers.fuelleVorlage(vorlage(praefix(b.getVerein().getName()) + "BG.dot"), werte);
    }

    /**
     * Spendenbescheinigung (Vorlagen FHSB*.dot bzw. FVSB*.dot je Träger und
     * Spendentyp). Bei Dauerspenden werden alle Einzelspenden des Jahres
     * summiert und als Liste am Lesezeichen {@code einzelbetrag} eingesetzt
     * (alt: fillDonationSummary), inklusive Betrag in Worten.
     */
    public byte[] spendenBescheinigung(Long spendeId) {
        Spende spende = spenden.findById(spendeId)
                .orElseThrow(() -> new IllegalArgumentException("Spende " + spendeId + " nicht gefunden"));
        Mitglied m = spende.getMitglied();
        String spendentyp = spende.getSpendenart().getSpendentyp();
        String verein = spende.getVerein().getName();
        NumberFormat zahl = NumberFormat.getNumberInstance(Locale.GERMANY);
        zahl.setMinimumFractionDigits(2);
        zahl.setMaximumFractionDigits(2);

        List<Spende> einzelspenden = "Geldspende dauer".equalsIgnoreCase(spendentyp)
                ? spenden.findJahresspenden(m.getId(), spende.getDatum().getYear(), spendentyp, verein)
                : List.of(spende);
        BigDecimal summe = einzelspenden.stream()
                .map(Spende::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, String> werte = new HashMap<>();
        werte.put("bescheinigung", spende.getSpendenart().getName());
        werte.put("vorname", m.getVorname());
        werte.put("name", m.getName());
        werte.put("strasse", m.getStrasse());
        werte.put("plz", m.getPlz());
        werte.put("ort", m.getOrt());
        werte.put("betrag", zahl.format(summe));
        werte.put("worte", BetragInWorten.ohneWaehrung(summe));
        werte.put("datum", DATUM.format(spende.getDatum()));
        werte.put("einzelbetrag", einzelspenden.stream()
                .map(s -> DATUM.format(s.getDatum()) + "\t" + zahl.format(s.getBetrag()))
                .collect(Collectors.joining("\r")));

        return DocumentCreationHelpers.fuelleVorlage(spendenVorlage(verein, spendentyp), werte);
    }

    /** FHSB<Typ>.dot bzw. FVSB<Typ>.dot; fehlt die typspezifische Vorlage, die allgemeine FHSB.dot/FVSB.dot. */
    private Path spendenVorlage(String verein, String spendentyp) {
        String praefix = praefix(verein);
        String zusatz = VORLAGE_JE_SPENDENTYP.getOrDefault(spendentyp, "");
        Path spezifisch = vorlagen.resolve(praefix + "SB" + zusatz + ".dot");
        return Files.exists(spezifisch) ? spezifisch : vorlage(praefix + "SB.dot");
    }

    /** Vorlagen-Präfix des Trägers: Förderverein -> FV, sonst FH. */
    private static String praefix(String verein) {
        return "Förderverein".equals(verein) ? "FV" : "FH";
    }

    private Path vorlage(String dateiname) {
        Path pfad = vorlagen.resolve(dateiname);
        if (!Files.exists(pfad)) {
            throw new IllegalStateException("Vorlage " + pfad + " nicht gefunden – app.vorlagen.pfad prüfen");
        }
        return pfad;
    }

    /** Hinweistext zum noch offenen Betrag (alt: Restsummen-Logik der Bußgeldbestätigung). */
    private static String restsummenText(BigDecimal betrag, BigDecimal gezahlt, NumberFormat waehrung) {
        if (gezahlt.signum() == 0) {
            return "Es wurden noch keine Zahlungen geleistet.";
        }
        BigDecimal rest = betrag.subtract(gezahlt);
        if (rest.signum() <= 0) {
            return "Das Bußgeld ist damit vollständig abbezahlt.";
        }
        return "Es stehen noch Zahlungen in Höhe von " + waehrung.format(rest) + " aus.";
    }
}
