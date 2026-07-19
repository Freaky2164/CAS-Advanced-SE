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
 * Erzeugt Dokumente aus den Word-Vorlagen im Vorlagen-Verzeichnis:
 * Bußgeldbestätigungen aus FHBG.dot bzw. FVBG.dot, Spendenbescheinigungen aus
 * den FHSB- bzw. FVSB-Vorlagen je Träger und Spendentyp. Befüllt werden die
 * Lesezeichen der Vorlagen, das Ergebnis ist ein Word-Dokument (.doc).
 *
 * @author Robin
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

    /**
     * Erzeugt den Service mit den Repositories und dem konfigurierten
     * Vorlagen-Verzeichnis.
     *
     * @param bussgelder das Bußgeld-Repository
     * @param spenden das Spenden-Repository
     * @param vorlagenPfad der Pfad zum Vorlagen-Verzeichnis
     */
    public DocumentCreationService(BussgeldRepository bussgelder,
                                   SpendeRepository spenden,
                                   @Value("${app.vorlagen.pfad}") String vorlagenPfad) {
        this.bussgelder = bussgelder;
        this.spenden = spenden;
        this.vorlagen = Path.of(vorlagenPfad);
    }

    /**
     * Erzeugt die Zahlungsbestätigung an das Gericht (Vorlage FHBG.dot bzw.
     * FVBG.dot) mit Anschrift des Gerichts, Strafsache, Bußgeldbetrag sowie
     * der Liste der Zahlungseingänge mit Restsummen-Hinweis.
     *
     * @param bussgeldId die ID des Bußgelds
     * @return die Bestätigung als .doc-Datei
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
        werte.put("restsumme", restsummenText(b.getBetrag(), gezahlt, b.isBezahlt(), waehrung));

        return DocumentCreationHelpers.fuelleVorlage(vorlage(praefix(b.getVerein().getName()) + "BG.dot"), werte);
    }

    /**
     * Erzeugt die Spendenbescheinigung (Vorlagen FHSB*.dot bzw. FVSB*.dot je
     * Träger und Spendentyp). Bei Dauerspenden werden alle Einzelspenden des
     * Jahres summiert und als Liste am Lesezeichen {@code einzelbetrag}
     * eingesetzt, inklusive Betrag in Worten.
     *
     * @param spendeId die ID der Spende
     * @return die Bescheinigung als .doc-Datei
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

    /**
     * Ermittelt die Spenden-Vorlage FHSB&lt;Typ&gt;.dot bzw.
     * FVSB&lt;Typ&gt;.dot; fehlt die typspezifische Vorlage, wird die
     * allgemeine FHSB.dot bzw. FVSB.dot verwendet.
     */
    private Path spendenVorlage(String verein, String spendentyp) {
        String praefix = praefix(verein);
        String zusatz = VORLAGE_JE_SPENDENTYP.getOrDefault(spendentyp, "");
        Path spezifisch = vorlagen.resolve(praefix + "SB" + zusatz + ".dot");
        return Files.exists(spezifisch) ? spezifisch : vorlage(praefix + "SB.dot");
    }

    /**
     * Liefert das Vorlagen-Präfix des Trägers: Förderverein -> FV, sonst FH.
     */
    private static String praefix(String verein) {
        return "Förderverein".equals(verein) ? "FV" : "FH";
    }

    /**
     * Liefert den absoluten Pfad einer Vorlage; schlägt mit klarer Meldung
     * fehl, wenn sie fehlt.
     */
    private Path vorlage(String dateiname) {
        Path pfad = vorlagen.resolve(dateiname);
        if (!Files.exists(pfad)) {
            throw new IllegalStateException("Vorlage " + pfad + " nicht gefunden – app.vorlagen.pfad prüfen");
        }
        return pfad;
    }

    /**
     * Liefert den Hinweistext zum Zahlungsstand. Ist das Bußgeld als bezahlt
     * gekennzeichnet, ohne dass dazu (ausreichende) Einzelzahlungen erfasst
     * sind, wird das ausdrücklich unterschieden, statt einen Zahlungseingang
     * auszuweisen, der nicht belegt ist.
     *
     * @param betrag der geschuldete Betrag
     * @param gezahlt die Summe der erfassten Zahlungseingänge
     * @param bezahlt das Kennzeichen, ob das Bußgeld als bezahlt gilt
     * @param waehrung das Format für Geldbeträge
     * @return der Hinweistext für die Bestätigung
     */
    private static String restsummenText(BigDecimal betrag, BigDecimal gezahlt, boolean bezahlt,
                                         NumberFormat waehrung) {
        if (gezahlt.signum() == 0) {
            return bezahlt
                    ? "Das Bußgeld ist als bezahlt gekennzeichnet; im System sind keine Einzelzahlungen erfasst."
                    : "Es wurden noch keine Zahlungen geleistet.";
        }
        BigDecimal rest = betrag.subtract(gezahlt);
        if (rest.signum() <= 0) {
            return "Das Bußgeld ist damit vollständig abbezahlt.";
        }
        if (bezahlt) {
            return "Das Bußgeld ist als bezahlt gekennzeichnet; erfasst sind Einzelzahlungen über "
                    + waehrung.format(gezahlt) + " von " + waehrung.format(betrag) + ".";
        }
        return "Es stehen noch Zahlungen in Höhe von " + waehrung.format(rest) + " aus.";
    }
}
