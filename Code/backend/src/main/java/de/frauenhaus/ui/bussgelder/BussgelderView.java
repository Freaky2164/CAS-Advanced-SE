package de.frauenhaus.ui.bussgelder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.domain.Bussgeldstatus;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.BussgeldService;
import de.frauenhaus.service.BussgeldService.BussgeldResponse;
import de.frauenhaus.service.BussgeldService.EingangResponse;
import de.frauenhaus.service.BussgeldstatusService;
import de.frauenhaus.service.DocumentCreationService;
import de.frauenhaus.service.DokumentService;
import de.frauenhaus.service.GerichtService;
import de.frauenhaus.service.VereinService;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.DokumenteDialog;
import de.frauenhaus.ui.support.UiUtil;
import de.frauenhaus.ui.support.VerlaufDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * @author Nils
 *
 * Pflege der Bußgelder: durchsuchbare Liste mit Anlegen, Bearbeiten, Löschen,
 * Zahlungseingängen, Zahlungsbestätigung als Word-Download sowie Dokumenten
 * und Änderungsverlauf.
 */
@Route(value = "bussgelder", layout = MainLayout.class)
@PageTitle("Bußgelder | Frauenhaus Verwaltung")
@PermitAll
public class BussgelderView extends VerticalLayout {

    private final transient BussgeldService bussgeldService;
    private final transient GerichtService gerichtService;
    private final transient VereinService vereinService;
    private final transient BussgeldstatusService bussgeldstatusService;
    private final transient DocumentCreationService documentCreationService;
    private final transient AuditService auditService;
    private final transient DokumentService dokumentService;

    private final TextField suche = new TextField();
    private final Grid<BussgeldResponse> grid = new Grid<>();

    public BussgelderView(BussgeldService bussgeldService, GerichtService gerichtService,
                          VereinService vereinService, BussgeldstatusService bussgeldstatusService,
                          DocumentCreationService documentCreationService,
                          AuditService auditService, DokumentService dokumentService) {
        this.bussgeldService = bussgeldService;
        this.gerichtService = gerichtService;
        this.vereinService = vereinService;
        this.bussgeldstatusService = bussgeldstatusService;
        this.documentCreationService = documentCreationService;
        this.auditService = auditService;
        this.dokumentService = dokumentService;

        setSizeFull();
        add(werkzeugleiste(), grid);
        gridAufbauen();
    }

    private HorizontalLayout werkzeugleiste() {
        suche.setPlaceholder("Suche (Gericht, Aktenzeichen …)");
        suche.setClearButtonVisible(true);
        suche.setValueChangeMode(ValueChangeMode.LAZY);
        suche.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        suche.setWidth("22em");

        Button neu = new Button("Neu", e -> bearbeiten(null));
        neu.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button bearbeiten = new Button("Bearbeiten", e -> auswahl().ifPresent(this::bearbeiten));
        Button loeschen = new Button("Löschen", e -> auswahl().ifPresent(this::loeschenBestaetigen));
        loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button dokumente = new Button("Dokumente", e -> auswahl().ifPresent(
                b -> new DokumenteDialog(dokumentService, "BUSSGELD", Long.toString(b.id()), beschreibung(b)).open()));
        Button verlauf = new Button("Verlauf", e -> auswahl().ifPresent(this::verlaufAnzeigen));

        Anchor bestaetigung = UiUtil.downloadLink("Zahlungsbestätigung", () -> {
            BussgeldResponse b = grid.asSingleSelect().getOptionalValue()
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Bitte zuerst ein Bußgeld auswählen"));
            return new UiUtil.Datei("bestaetigung-" + b.id() + ".doc", UiUtil.DOC,
                    documentCreationService.bussgeldBestaetigung(b.id()));
        });
        bestaetigung.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> bestaetigung.setEnabled(e.getValue() != null));

        HorizontalLayout leiste = new HorizontalLayout(suche, neu, bearbeiten, loeschen, dokumente, verlauf, bestaetigung);
        leiste.setAlignItems(Alignment.CENTER);
        return leiste;
    }

    private void gridAufbauen() {
        grid.addColumn(BussgeldResponse::id).setHeader("Nr.").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(b -> UiUtil.datum(b.datum())).setHeader("Datum").setAutoWidth(true);
        grid.addColumn(BussgeldResponse::gerichtBezeichnung).setHeader("Gericht").setAutoWidth(true);
        grid.addColumn(BussgeldResponse::aktenzeichen).setHeader("Aktenzeichen").setAutoWidth(true);
        grid.addColumn(BussgeldResponse::verein).setHeader("Träger").setAutoWidth(true);
        grid.addColumn(BussgeldResponse::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(b -> UiUtil.betrag(b.betrag())).setHeader("Betrag").setAutoWidth(true);
        grid.addColumn(b -> b.bezahlt() ? "✓" : "").setHeader("Bezahlt").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(b -> UiUtil.datum(b.zieldatum())).setHeader("Zieldatum").setAutoWidth(true);
        grid.setItems(query -> bussgeldService.alle(
                        PageRequest.of(query.getPage(), query.getPageSize(), Sort.by(Sort.Direction.DESC, "datum")),
                        suche.getValue())
                .stream());
        grid.setSizeFull();
    }

    private java.util.Optional<BussgeldResponse> auswahl() {
        java.util.Optional<BussgeldResponse> auswahl = grid.asSingleSelect().getOptionalValue();
        if (auswahl.isEmpty()) {
            UiUtil.fehler(new IllegalStateException("Bitte zuerst ein Bußgeld auswählen"));
        }
        return auswahl;
    }

    private void bearbeiten(BussgeldResponse vorhanden) {
        new BussgeldDialog(vorhanden).open();
    }

    private void loeschenBestaetigen(BussgeldResponse b) {
        ConfirmDialog dialog = new ConfirmDialog("Bußgeld löschen",
                "Soll das Bußgeld Nr. " + b.id() + " (" + beschreibung(b) + ") wirklich gelöscht werden?",
                "Löschen", e -> {
                    try {
                        bussgeldService.loeschen(b.id());
                        grid.getDataProvider().refreshAll();
                        UiUtil.erfolg("Bußgeld gelöscht");
                    } catch (Exception ex) {
                        UiUtil.fehler(ex);
                    }
                },
                "Abbrechen", e -> {
                });
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void verlaufAnzeigen(BussgeldResponse b) {
        try {
            new VerlaufDialog(beschreibung(b), auditService.verlauf(Bussgeld.class, b.id())).open();
        } catch (Exception e) {
            UiUtil.fehler(e);
        }
    }

    private static String beschreibung(BussgeldResponse b) {
        return b.gerichtBezeichnung() + ", " + UiUtil.datum(b.datum()) + ", " + UiUtil.betrag(b.betrag());
    }

    /**
     * @author Nils
     *
     * Bearbeitungsdialog für ein Bußgeld; bei bestehenden Bußgeldern können
     * zusätzlich Zahlungseingänge erfasst und entfernt werden.
     */
    private final class BussgeldDialog extends Dialog {

        private BussgeldResponse vorhanden;

        private final ComboBox<Gericht> gericht = new ComboBox<>("Gericht");
        private final ComboBox<String> verein = new ComboBox<>("Träger");
        private final ComboBox<String> status = new ComboBox<>("Status");
        private final TextField name = new TextField("Name");
        private final TextField vorname = new TextField("Vorname");
        private final TextField aktenzeichen = new TextField("Aktenzeichen");
        private final DatePicker datum = new DatePicker("Datum");
        private final DatePicker zieldatum = new DatePicker("Zieldatum");
        private final BigDecimalField betrag = new BigDecimalField("Betrag (€)");
        private final Checkbox bezahlt = new Checkbox("Bezahlt");
        private final TextArea bemerkung = new TextArea("Bemerkung");

        private final Grid<EingangResponse> eingaenge = new Grid<>();
        private final DatePicker eingangDatum = new DatePicker("Datum");
        private final BigDecimalField eingangBetrag = new BigDecimalField("Betrag (€)");
        private final TextField eingangBemerkung = new TextField("Bemerkung");

        private BussgeldDialog(BussgeldResponse vorhanden) {
            this.vorhanden = vorhanden;
            setHeaderTitle(vorhanden == null ? "Bußgeld anlegen" : "Bußgeld bearbeiten (Nr. " + vorhanden.id() + ")");
            setWidth("52em");

            gericht.setItems(gerichtService.alle(null));
            gericht.setItemLabelGenerator(Gericht::getBezeichnung);
            verein.setItems(vereinService.alle(null).stream().map(Verein::getName).toList());
            status.setItems(bussgeldstatusService.alle().stream().map(Bussgeldstatus::getName).toList());
            status.setClearButtonVisible(true);
            datum.setLocale(Locale.GERMANY);
            zieldatum.setLocale(Locale.GERMANY);
            eingangDatum.setLocale(Locale.GERMANY);

            if (vorhanden != null) {
                gerichtService.alle(null).stream()
                        .filter(g -> g.getId().equals(vorhanden.gerichtId()))
                        .findFirst()
                        .ifPresent(gericht::setValue);
                verein.setValue(vorhanden.verein());
                if (vorhanden.status() != null) {
                    status.setValue(vorhanden.status());
                }
                name.setValue(vorhanden.name() != null ? vorhanden.name() : "");
                vorname.setValue(vorhanden.vorname() != null ? vorhanden.vorname() : "");
                aktenzeichen.setValue(vorhanden.aktenzeichen() != null ? vorhanden.aktenzeichen() : "");
                datum.setValue(vorhanden.datum());
                zieldatum.setValue(vorhanden.zieldatum());
                betrag.setValue(vorhanden.betrag());
                bezahlt.setValue(vorhanden.bezahlt());
                bemerkung.setValue(vorhanden.bemerkung() != null ? vorhanden.bemerkung() : "");
            }

            FormLayout form = new FormLayout();
            form.add(gericht, verein, status, aktenzeichen, name, vorname,
                    datum, zieldatum, betrag, bezahlt, bemerkung);
            form.setColspan(bemerkung, 2);
            add(form);

            if (vorhanden != null) {
                add(eingaengeBereich());
            }

            Button speichern = new Button("Speichern", e -> speichern());
            speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        }

        private VerticalLayout eingaengeBereich() {
            eingaenge.addColumn(e -> UiUtil.datum(e.datum())).setHeader("Datum").setAutoWidth(true);
            eingaenge.addColumn(e -> UiUtil.betrag(e.betrag())).setHeader("Betrag").setAutoWidth(true);
            eingaenge.addColumn(EingangResponse::bemerkung).setHeader("Bemerkung").setFlexGrow(1);
            eingaenge.addColumn(new ComponentRenderer<>(this::eingangEntfernenButton)).setHeader("").setAutoWidth(true);
            eingaenge.setAllRowsVisible(true);
            eingaenge.setItems(vorhanden.eingaenge());

            Button hinzufuegen = new Button("Eingang hinzufügen", e -> eingangHinzufuegen());
            HorizontalLayout eingabe = new HorizontalLayout(eingangDatum, eingangBetrag, eingangBemerkung, hinzufuegen);
            eingabe.setAlignItems(Alignment.END);

            VerticalLayout bereich = new VerticalLayout(new H4("Zahlungseingänge"), eingaenge, eingabe);
            bereich.setPadding(false);
            return bereich;
        }

        private Button eingangEntfernenButton(EingangResponse eingang) {
            Button button = new Button("Entfernen", e -> {
                try {
                    vorhanden = bussgeldService.eingangEntfernen(vorhanden.id(), eingang.id());
                    eingaenge.setItems(vorhanden.eingaenge());
                    grid.getDataProvider().refreshAll();
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            });
            button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return button;
        }

        private void eingangHinzufuegen() {
            if (eingangDatum.getValue() == null || eingangBetrag.getValue() == null) {
                UiUtil.fehler(new IllegalStateException("Bitte Datum und Betrag des Eingangs angeben"));
                return;
            }
            try {
                String bemerkungWert = eingangBemerkung.getValue().isBlank() ? null : eingangBemerkung.getValue().trim();
                vorhanden = bussgeldService.eingangHinzufuegen(vorhanden.id(),
                        eingangDatum.getValue(), eingangBetrag.getValue(), bemerkungWert);
                eingaenge.setItems(vorhanden.eingaenge());
                eingangDatum.clear();
                eingangBetrag.clear();
                eingangBemerkung.clear();
                grid.getDataProvider().refreshAll();
            } catch (Exception e) {
                UiUtil.fehler(e);
            }
        }

        private void speichern() {
            if (gericht.getValue() == null || verein.getValue() == null
                    || datum.getValue() == null || betrag.getValue() == null) {
                UiUtil.fehler(new IllegalStateException("Bitte Gericht, Träger, Datum und Betrag angeben"));
                return;
            }
            try {
                if (vorhanden == null) {
                    bussgeldService.anlegen(gericht.getValue().getId(), verein.getValue(), status.getValue(),
                            wertOderNull(name.getValue()), wertOderNull(vorname.getValue()),
                            wertOderNull(aktenzeichen.getValue()), datum.getValue(), zieldatum.getValue(),
                            betrag.getValue(), bezahlt.getValue(), wertOderNull(bemerkung.getValue()));
                    UiUtil.erfolg("Bußgeld angelegt");
                } else {
                    bussgeldService.aendern(vorhanden.id(), gericht.getValue().getId(), verein.getValue(),
                            status.getValue(), wertOderNull(name.getValue()), wertOderNull(vorname.getValue()),
                            wertOderNull(aktenzeichen.getValue()), datum.getValue(), zieldatum.getValue(),
                            betrag.getValue(), bezahlt.getValue(), wertOderNull(bemerkung.getValue()));
                    UiUtil.erfolg("Bußgeld gespeichert");
                }
                grid.getDataProvider().refreshAll();
                close();
            } catch (Exception e) {
                UiUtil.fehler(e);
            }
        }
    }

    private static String wertOderNull(String wert) {
        return wert == null || wert.isBlank() ? null : wert.trim();
    }
}
