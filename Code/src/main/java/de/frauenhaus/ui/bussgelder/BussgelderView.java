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
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.domain.Bussgeldstatus;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.BussgeldService;
import de.frauenhaus.service.BussgeldService.BussgeldDaten;
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
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Pflege der Bußgelder: durchsuchbare Liste mit Anlegen, Bearbeiten, Löschen,
 * Zahlungseingängen, Zahlungsbestätigung als Word-Download sowie Dokumenten
 * und Änderungsverlauf.
 *
 * @author Paul
 */
@Route(value = "bussgelder", layout = MainLayout.class)
@PageTitle("Bußgelder | Frauenhaus Verwaltung")
@PermitAll
public class BussgelderView extends VerticalLayout {

    private static final String DATUM_LABEL = "Datum";
    private static final String BEMERKUNG_LABEL = "Bemerkung";

    private final transient BussgeldService bussgeldService;
    private final transient GerichtService gerichtService;
    private final transient VereinService vereinService;
    private final transient BussgeldstatusService bussgeldstatusService;
    private final transient DocumentCreationService documentCreationService;
    private final transient DokumentService dokumentService;

    private final TextField suche = new TextField();
    private final Grid<BussgeldResponse> grid = new Grid<>();

    /**
     * Baut die Bußgeld-Seite mit Werkzeugleiste und Liste auf.
     *
     * @param bussgeldService der Service für die Bußgeld-Pflege
     * @param gerichtService der Service für die Gerichtsauswahl
     * @param vereinService der Service für die Trägerauswahl
     * @param bussgeldstatusService der Service für die Statusauswahl
     * @param documentCreationService der Service für die Zahlungsbestätigung
     * @param dokumentService der Service für Dokument-Anhänge
     */
    public BussgelderView(BussgeldService bussgeldService, GerichtService gerichtService,
                          VereinService vereinService, BussgeldstatusService bussgeldstatusService,
                          DocumentCreationService documentCreationService,
                          DokumentService dokumentService) {
        this.bussgeldService = bussgeldService;
        this.gerichtService = gerichtService;
        this.vereinService = vereinService;
        this.bussgeldstatusService = bussgeldstatusService;
        this.documentCreationService = documentCreationService;
        this.dokumentService = dokumentService;

        setSizeFull();
        add(werkzeugleiste(), grid);
        gridAufbauen();
    }

    /**
     * Baut die Werkzeugleiste mit Suche, Aktions-Buttons und dem Download-Link
     * für die Zahlungsbestätigung auf.
     */
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

    /**
     * Konfiguriert die Spalten und die seitenweise Datenanbindung der Liste.
     */
    private void gridAufbauen() {
        grid.addColumn(BussgeldResponse::id).setHeader("Nr.").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(b -> UiUtil.datum(b.datum())).setHeader(DATUM_LABEL).setAutoWidth(true);
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
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
    }

    /**
     * Liefert das ausgewählte Bußgeld oder zeigt einen Hinweis an.
     */
    private java.util.Optional<BussgeldResponse> auswahl() {
        java.util.Optional<BussgeldResponse> auswahl = grid.asSingleSelect().getOptionalValue();
        if (auswahl.isEmpty()) {
            UiUtil.fehler(new IllegalStateException("Bitte zuerst ein Bußgeld auswählen"));
        }
        return auswahl;
    }

    /**
     * Öffnet den Bearbeitungsdialog; {@code null} legt ein neues Bußgeld an.
     */
    private void bearbeiten(BussgeldResponse vorhanden) {
        new BussgeldDialog(vorhanden).open();
    }

    /**
     * Fragt die Löschung ab und löscht das Bußgeld nach Bestätigung.
     */
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

    /**
     * Öffnet den Zahlungsverlauf des Bußgelds mit allen erfassten
     * Zahlungseingängen in chronologischer Reihenfolge.
     */
    private void verlaufAnzeigen(BussgeldResponse b) {
        try {
            List<EingangResponse> eingaengeSortiert = bussgeldService.finden(b.id()).eingaenge().stream()
                    .sorted(Comparator.comparing(EingangResponse::datum))
                    .toList();

            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Zahlungsverlauf: " + beschreibung(b));
            dialog.setWidth("40em");

            if (eingaengeSortiert.isEmpty()) {
                dialog.add(new Span("Für dieses Bußgeld sind keine Zahlungseingänge erfasst."));
            } else {
                Grid<EingangResponse> verlauf = new Grid<>();
                verlauf.addColumn(e -> UiUtil.datum(e.datum())).setHeader(DATUM_LABEL).setAutoWidth(true);
                verlauf.addColumn(e -> UiUtil.betrag(e.betrag())).setHeader("Betrag").setAutoWidth(true);
                verlauf.addColumn(EingangResponse::bemerkung).setHeader(BEMERKUNG_LABEL).setFlexGrow(1);
                verlauf.setItems(eingaengeSortiert);
                verlauf.setAllRowsVisible(true);
                dialog.add(verlauf);
            }

            dialog.getFooter().add(new Button("Schließen", e -> dialog.close()));
            dialog.open();
        } catch (Exception e) {
            UiUtil.fehler(e);
        }
    }

    /**
     * Liefert eine Kurzbeschreibung des Bußgelds für Dialogtitel.
     */
    private static String beschreibung(BussgeldResponse b) {
        return b.gerichtBezeichnung() + ", " + UiUtil.datum(b.datum()) + ", " + UiUtil.betrag(b.betrag());
    }

    /**
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
        private final DatePicker datum = new DatePicker(DATUM_LABEL);
        private final DatePicker zieldatum = new DatePicker("Zieldatum");
        private final BigDecimalField betrag = new BigDecimalField("Betrag (€)");
        private final Checkbox bezahlt = new Checkbox("Bezahlt");
        private final TextArea bemerkung = new TextArea(BEMERKUNG_LABEL);

        private final Grid<EingangResponse> eingaenge = new Grid<>();
        private final DatePicker eingangDatum = new DatePicker(DATUM_LABEL);
        private final BigDecimalField eingangBetrag = new BigDecimalField("Betrag (€)");
        private final TextField eingangBemerkung = new TextField(BEMERKUNG_LABEL);

        /**
         * Baut den Dialog auf und füllt bei bestehenden Bußgeldern die Felder vor.
         *
         * @param vorhanden das zu bearbeitende Bußgeld oder {@code null} zum Anlegen
         */
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

        /**
         * Baut den Bereich zur Anzeige und Erfassung der Zahlungseingänge auf.
         */
        private VerticalLayout eingaengeBereich() {
            eingaenge.addColumn(e -> UiUtil.datum(e.datum())).setHeader(DATUM_LABEL).setAutoWidth(true);
            eingaenge.addColumn(e -> UiUtil.betrag(e.betrag())).setHeader("Betrag").setAutoWidth(true);
            eingaenge.addColumn(EingangResponse::bemerkung).setHeader(BEMERKUNG_LABEL).setFlexGrow(1);
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

        /**
         * Erzeugt den Button zum Entfernen eines Zahlungseingangs.
         */
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

        /**
         * Erfasst einen neuen Zahlungseingang aus den Eingabefeldern.
         */
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

        /**
         * Validiert die Eingaben und legt das Bußgeld an bzw. speichert es.
         */
        private void speichern() {
            if (gericht.getValue() == null || verein.getValue() == null
                    || datum.getValue() == null || betrag.getValue() == null) {
                UiUtil.fehler(new IllegalStateException("Bitte Gericht, Träger, Datum und Betrag angeben"));
                return;
            }
            try {
                BussgeldDaten daten = new BussgeldDaten(gericht.getValue().getId(), verein.getValue(),
                        status.getValue(), wertOderNull(name.getValue()), wertOderNull(vorname.getValue()),
                        wertOderNull(aktenzeichen.getValue()), datum.getValue(), zieldatum.getValue(),
                        betrag.getValue(), bezahlt.getValue(), wertOderNull(bemerkung.getValue()));
                if (vorhanden == null) {
                    bussgeldService.anlegen(daten);
                    UiUtil.erfolg("Bußgeld angelegt");
                } else {
                    bussgeldService.aendern(vorhanden.id(), daten);
                    UiUtil.erfolg("Bußgeld gespeichert");
                }
                grid.getDataProvider().refreshAll();
                close();
            } catch (Exception e) {
                UiUtil.fehler(e);
            }
        }
    }

    /**
     * Trimmt den Wert und liefert {@code null}, wenn er leer ist.
     */
    private static String wertOderNull(String wert) {
        return wert == null || wert.isBlank() ? null : wert.trim();
    }
}
