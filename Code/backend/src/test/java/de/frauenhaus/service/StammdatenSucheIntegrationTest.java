package de.frauenhaus.service;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Bussgeldstatus;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.domain.Spendentyp;
import de.frauenhaus.domain.Stichwort;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.repository.BussgeldstatusRepository;
import de.frauenhaus.repository.BussgeldRepository;
import de.frauenhaus.repository.GerichtRepository;
import de.frauenhaus.repository.MitgliedRepository;
import de.frauenhaus.repository.SpendeRepository;
import de.frauenhaus.repository.SpendenartRepository;
import de.frauenhaus.repository.SpendentypRepository;
import de.frauenhaus.repository.StichwortRepository;
import de.frauenhaus.repository.VereinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nils
 *
 * Integrationstests für die generische Stammdaten-Suche über die Listen-Endpunkte.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.security.initial-admin-password=admin")
class StammdatenSucheIntegrationTest {

    @Autowired
    private MitgliedService mitgliedService;
    @Autowired
    private SpendeService spendeService;
    @Autowired
    private BussgeldService bussgeldService;
    @Autowired
    private VereinService vereinService;
    @Autowired
    private GerichtService gerichtService;

    @Autowired
    private MitgliedRepository mitglieder;
    @Autowired
    private StichwortRepository stichworte;
    @Autowired
    private SpendeRepository spenden;
    @Autowired
    private SpendenartRepository spendenarten;
    @Autowired
    private BussgeldRepository bussgelder;
    @Autowired
    private SpendentypRepository spendentypen;
    @Autowired
    private BussgeldstatusRepository bussgeldstati;
    @Autowired
    private VereinRepository vereine;
    @Autowired
    private GerichtRepository gerichte;

    @Test
    void mitgliederSucheFiltertUeberStichwortUndIstCaseInsensitive() {
        String suffix = suffix();
        Mitglied treffer = neuesMitglied("Anna", "MitgliedSuche" + suffix, "Zuerich", "mitglied-" + suffix + "@test.invalid");
        treffer.getStichworte().add(stichworte.save(new Stichwort("AlphaSuche" + suffix)));
        mitglieder.saveAndFlush(treffer);

        Mitglied ohneTreffer = neuesMitglied("Berta", "OhneTreffer" + suffix, "Bern", "ohne-" + suffix + "@test.invalid");
        mitglieder.saveAndFlush(ohneTreffer);

        var seite = mitgliedService.alle(PageRequest.of(0, 20), ("alphasuche" + suffix).toLowerCase());
        assertThat(seite.getContent()).extracting(MitgliedService.MitgliedResponse::id).containsExactly(treffer.getId());

        long erwartet = mitglieder.count();
        assertThat(mitgliedService.alle(PageRequest.of(0, 20), null).getTotalElements()).isEqualTo(erwartet);
        assertThat(mitgliedService.alle(PageRequest.of(0, 20), "   ").getTotalElements()).isEqualTo(erwartet);
    }

    @Test
    void spendenSucheFiltertNachMitgliedUndIstCaseInsensitive() {
        String suffix = suffix();
        Verein verein = vereine.saveAndFlush(new Verein("SV" + suffix, "Spendenverein " + suffix));
        Spendentyp spendentyp = spendentypen.saveAndFlush(new Spendentyp("Geld " + suffix));
        Spendenart spendenart = spendenarten.saveAndFlush(new Spendenart("Geldspende " + suffix, spendentyp.getName()));
        Mitglied trefferMitglied = mitglieder.saveAndFlush(neuesMitglied("Clara", "SpendeSuche" + suffix, "Basel", null));
        Mitglied ohneTrefferMitglied = mitglieder.saveAndFlush(neuesMitglied("Dora", "AndereSpende" + suffix, "Luzern", null));

        spenden.saveAndFlush(neueSpende(trefferMitglied, spendenart, verein, "Bemerkung " + suffix));
        spenden.saveAndFlush(neueSpende(ohneTrefferMitglied, spendenart, verein, "Andere Bemerkung " + suffix));

        var seite = spendeService.alle(PageRequest.of(0, 20), ("spendesuche" + suffix).toLowerCase());
        assertThat(seite.getContent()).extracting(SpendeService.SpendeResponse::mitgliedName)
                .allMatch(name -> name.contains("SpendeSuche" + suffix))
                .hasSize(1);

        long erwartet = spenden.count();
        assertThat(spendeService.alle(PageRequest.of(0, 20), null).getTotalElements()).isEqualTo(erwartet);
        assertThat(spendeService.alle(PageRequest.of(0, 20), "").getTotalElements()).isEqualTo(erwartet);
    }

    @Test
    void bussgeldSucheFiltertNachAktenzeichenUndIstCaseInsensitive() {
        String suffix = suffix();
        Verein verein = vereine.saveAndFlush(new Verein("BV" + suffix, "Bussgeldverein " + suffix));
        Gericht gericht = neuesGericht("Amtsgericht " + suffix, "Koeln");
        gericht = gerichte.saveAndFlush(gericht);
        Bussgeldstatus status = bussgeldstati.saveAndFlush(new Bussgeldstatus("offen-" + suffix));

        Bussgeld treffer = neuesBussgeld(gericht, verein, status.getName(), "AZ-SUCHE-" + suffix, "Eva", "Treffer", "Hinweis " + suffix);
        Bussgeld ohneTreffer = neuesBussgeld(gericht, verein, status.getName(), "AZ-ANDERS-" + suffix, "Fina", "Ohne", "Anders " + suffix);
        bussgelder.saveAndFlush(treffer);
        bussgelder.saveAndFlush(ohneTreffer);

        var seite = bussgeldService.alle(PageRequest.of(0, 20), ("az-suche-" + suffix).toLowerCase());
        assertThat(seite.getContent()).extracting(BussgeldService.BussgeldResponse::aktenzeichen)
                .containsExactly(treffer.getAktenzeichen());

        long erwartet = bussgelder.count();
        assertThat(bussgeldService.alle(PageRequest.of(0, 20), null).getTotalElements()).isEqualTo(erwartet);
        assertThat(bussgeldService.alle(PageRequest.of(0, 20), " ").getTotalElements()).isEqualTo(erwartet);
    }

    @Test
    void vereineSucheFiltertNachBezeichnungUndIstCaseInsensitive() {
        String suffix = suffix();
        Verein treffer = vereine.saveAndFlush(new Verein("VV" + suffix, "Foerderverein Suche " + suffix));
        vereine.saveAndFlush(new Verein("VW" + suffix, "Anderer Verein " + suffix));

        List<Verein> ergebnis = vereinService.alle(("foerderverein suche " + suffix).toLowerCase());
        assertThat(ergebnis).extracting(Verein::getName).containsExactly(treffer.getName());

        assertThat(vereinService.alle(null)).hasSize((int) vereine.count());
        assertThat(vereinService.alle("   ")).hasSize((int) vereine.count());
    }

    @Test
    void gerichteSucheFiltertNachOrtUndIstCaseInsensitive() {
        String suffix = suffix();
        Gericht treffer = gerichte.saveAndFlush(neuesGericht("Landgericht " + suffix, "HamburgSuche" + suffix));
        gerichte.saveAndFlush(neuesGericht("Amtsgericht " + suffix, "Muenchen" + suffix));

        List<Gericht> ergebnis = gerichtService.alle(("hamburgsuche" + suffix).toLowerCase());
        assertThat(ergebnis).extracting(Gericht::getId).containsExactly(treffer.getId());

        assertThat(gerichtService.alle(null)).hasSize((int) gerichte.count());
        assertThat(gerichtService.alle("")).hasSize((int) gerichte.count());
    }

    private Mitglied neuesMitglied(String vorname, String name, String ort, String email) {
        Mitglied m = new Mitglied();
        m.setVorname(vorname);
        m.setName(name);
        m.setOrt(ort);
        m.setEmail(email);
        return m;
    }

    private Spende neueSpende(Mitglied mitglied, Spendenart spendenart, Verein verein, String bemerkung) {
        Spende spende = new Spende();
        spende.setMitglied(mitglied);
        spende.setSpendenart(spendenart);
        spende.setVerein(verein);
        spende.setDatum(LocalDate.of(2026, 7, 12));
        spende.setBetrag(new BigDecimal("25.00"));
        spende.setBemerkung(bemerkung);
        return spende;
    }

    private Gericht neuesGericht(String bezeichnung, String ort) {
        Gericht gericht = new Gericht();
        gericht.setBezeichnung(bezeichnung);
        gericht.setStrasse("Teststrasse 1");
        gericht.setPlz("12345");
        gericht.setOrt(ort);
        return gericht;
    }

    private Bussgeld neuesBussgeld(Gericht gericht, Verein verein, String status, String aktenzeichen,
                                   String vorname, String name, String bemerkung) {
        Bussgeld bussgeld = new Bussgeld();
        bussgeld.setGericht(gericht);
        bussgeld.setVerein(verein);
        bussgeld.setStatus(status);
        bussgeld.setAktenzeichen(aktenzeichen);
        bussgeld.setVorname(vorname);
        bussgeld.setName(name);
        bussgeld.setDatum(LocalDate.of(2026, 7, 12));
        bussgeld.setZieldatum(LocalDate.of(2026, 8, 12));
        bussgeld.setBetrag(new BigDecimal("50.00"));
        bussgeld.setBezahlt(false);
        bussgeld.setBemerkung(bemerkung);
        return bussgeld;
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
