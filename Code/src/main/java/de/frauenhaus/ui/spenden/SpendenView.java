package de.frauenhaus.ui.spenden;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.domain.Spende;
import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.DocumentCreationService;
import de.frauenhaus.service.DokumentService;
import de.frauenhaus.service.MitgliedService;
import de.frauenhaus.service.MitgliedService.MitgliedResponse;
import de.frauenhaus.service.SpendeService;
import de.frauenhaus.service.SpendeService.SpendeResponse;
import de.frauenhaus.service.SpendenartService;
import de.frauenhaus.service.VereinService;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.DokumenteDialog;
import de.frauenhaus.ui.support.UiUtil;
import de.frauenhaus.ui.support.VerlaufDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Pflege der Spenden: durchsuchbare Liste mit Anlegen, Bearbeiten, Löschen,
 * Spendenbescheinigung als Word-Download sowie Dokumenten und Verlauf.
 *
 * @author Paul
 */
@Route(value = "spenden", layout = MainLayout.class)
@PageTitle("Spenden | Frauenhaus Verwaltung")
@PermitAll
public class SpendenView extends VerticalLayout {

    private final transient SpendeService spendeService;
    private final transient MitgliedService mitgliedService;
    private final transient SpendenartService spendenartService;
    private final transient VereinService vereinService;
    private final transient DocumentCreationService documentCreationService;
    private final transient AuditService auditService;
    private final transient DokumentService dokumentService;

    private final TextField suche = new TextField();
    private final Grid<SpendeResponse> grid = new Grid<>();

    /**
     * Baut die Spenden-Seite mit Werkzeugleiste und Liste auf.
     *
     * @param spendeService der Service für die Spenden-Pflege
     * @param mitgliedService der Service für die Mitgliederauswahl
     * @param spendenartService der Service für die Spendenart-Auswahl
     * @param vereinService der Service für die Trägerauswahl
     * @param documentCreationService der Service für die Spendenbescheinigung
     * @param auditService der Service für den Änderungsverlauf
     * @param dokumentService der Service für Dokument-Anhänge
     */
    public SpendenView(SpendeService spendeService, MitgliedService mitgliedService,
                       SpendenartService spendenartService, VereinService vereinService,
                       DocumentCreationService documentCreationService,
                       AuditService auditService, DokumentService dokumentService) {
        this.spendeService = spendeService;
        this.mitgliedService = mitgliedService;
        this.spendenartService = spendenartService;
        this.vereinService = vereinService;
        this.documentCreationService = documentCreationService;
        this.auditService = auditService;
        this.dokumentService = dokumentService;

        setSizeFull();
        add(werkzeugleiste(), grid);
        gridAufbauen();
    }

    /**
     * Baut die Werkzeugleiste mit Suche, Aktions-Buttons und dem Download-Link
     * für die Spendenquittung auf.
     */
    private HorizontalLayout werkzeugleiste() {
        suche.setPlaceholder("Suche (Mitglied, Spendenart …)");
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
                s -> new DokumenteDialog(dokumentService, "SPENDE", Long.toString(s.id()), beschreibung(s)).open()));
        Button verlauf = new Button("Verlauf", e -> auswahl().ifPresent(this::verlaufAnzeigen));

        Anchor quittung = UiUtil.downloadLink("Spendenquittung", () -> {
            SpendeResponse s = grid.asSingleSelect().getOptionalValue()
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Bitte zuerst eine Spende auswählen"));
            return new UiUtil.Datei("spendenbescheinigung-" + s.id() + ".doc", UiUtil.DOC,
                    documentCreationService.spendenBescheinigung(s.id()));
        });
        quittung.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> quittung.setEnabled(e.getValue() != null));

        HorizontalLayout leiste = new HorizontalLayout(suche, neu, bearbeiten, loeschen, dokumente, verlauf, quittung);
        leiste.setAlignItems(Alignment.CENTER);
        return leiste;
    }

    /**
     * Konfiguriert die Spalten und die seitenweise Datenanbindung der Liste.
     */
    private void gridAufbauen() {
        grid.addColumn(SpendeResponse::id).setHeader("Nr.").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(s -> UiUtil.datum(s.datum())).setHeader("Datum").setAutoWidth(true);
        grid.addColumn(SpendeResponse::mitgliedName).setHeader("Mitglied").setAutoWidth(true);
        grid.addColumn(SpendeResponse::spendenart).setHeader("Spendenart").setAutoWidth(true);
        grid.addColumn(SpendeResponse::verein).setHeader("Träger").setAutoWidth(true);
        grid.addColumn(s -> UiUtil.betrag(s.betrag())).setHeader("Betrag").setAutoWidth(true);
        grid.addColumn(SpendeResponse::bemerkung).setHeader("Bemerkung");
        grid.setItems(query -> spendeService.alle(
                        PageRequest.of(query.getPage(), query.getPageSize(), Sort.by(Sort.Direction.DESC, "datum")),
                        suche.getValue())
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
    }

    /**
     * Liefert die ausgewählte Spende oder zeigt einen Hinweis an.
     */
    private java.util.Optional<SpendeResponse> auswahl() {
        java.util.Optional<SpendeResponse> auswahl = grid.asSingleSelect().getOptionalValue();
        if (auswahl.isEmpty()) {
            UiUtil.fehler(new IllegalStateException("Bitte zuerst eine Spende auswählen"));
        }
        return auswahl;
    }

    /**
     * Öffnet den Bearbeitungsdialog; {@code null} legt eine neue Spende an.
     */
    private void bearbeiten(SpendeResponse vorhanden) {
        new SpendeDialog(vorhanden).open();
    }

    /**
     * Fragt die Löschung ab und löscht die Spende nach Bestätigung.
     */
    private void loeschenBestaetigen(SpendeResponse s) {
        ConfirmDialog dialog = new ConfirmDialog("Spende löschen",
                "Soll die Spende Nr. " + s.id() + " (" + beschreibung(s) + ") wirklich gelöscht werden?",
                "Löschen", e -> {
                    try {
                        spendeService.loeschen(s.id());
                        grid.getDataProvider().refreshAll();
                        UiUtil.erfolg("Spende gelöscht");
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
     * Öffnet den Änderungsverlauf der Spende.
     */
    private void verlaufAnzeigen(SpendeResponse s) {
        try {
            new VerlaufDialog(beschreibung(s), auditService.verlauf(Spende.class, s.id())).open();
        } catch (Exception e) {
            UiUtil.fehler(e);
        }
    }

    /**
     * Liefert eine Kurzbeschreibung der Spende für Dialogtitel.
     */
    private static String beschreibung(SpendeResponse s) {
        return s.mitgliedName() + ", " + UiUtil.datum(s.datum()) + ", " + UiUtil.betrag(s.betrag());
    }

    /**
     * Bearbeitungsdialog für eine Spende; Mitglied per durchsuchbarer Auswahl.
     */
    private final class SpendeDialog extends Dialog {

        private final SpendeResponse vorhanden;

        private final ComboBox<MitgliedResponse> mitglied = new ComboBox<>("Mitglied");
        private final ComboBox<String> spendenart = new ComboBox<>("Spendenart");
        private final ComboBox<String> verein = new ComboBox<>("Träger");
        private final DatePicker datum = new DatePicker("Datum");
        private final BigDecimalField betrag = new BigDecimalField("Betrag (€)");
        private final TextArea bemerkung = new TextArea("Bemerkung");

        /**
         * Baut den Dialog auf und füllt bei bestehenden Spenden die Felder vor.
         *
         * @param vorhanden die zu bearbeitende Spende oder {@code null} zum Anlegen
         */
        private SpendeDialog(SpendeResponse vorhanden) {
            this.vorhanden = vorhanden;
            setHeaderTitle(vorhanden == null ? "Spende anlegen" : "Spende bearbeiten (Nr. " + vorhanden.id() + ")");
            setWidth("40em");

            mitglied.setItems(query -> mitgliedService.alle(
                            PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("name", "vorname")),
                            query.getFilter().orElse(null))
                    .stream());
            mitglied.setItemLabelGenerator(m -> m.id() + " – " + (m.vorname() != null ? m.vorname() + " " : "") + m.name());
            spendenart.setItems(spendenartService.alle().stream().map(Spendenart::getName).toList());
            verein.setItems(vereinService.alle(null).stream().map(Verein::getName).toList());
            datum.setLocale(Locale.GERMANY);
            datum.setValue(LocalDate.now(ZoneId.systemDefault()));

            if (vorhanden != null) {
                mitglied.setValue(mitgliedService.finden(vorhanden.mitgliedId()));
                spendenart.setValue(vorhanden.spendenart());
                verein.setValue(vorhanden.verein());
                datum.setValue(vorhanden.datum());
                betrag.setValue(vorhanden.betrag());
                bemerkung.setValue(vorhanden.bemerkung() != null ? vorhanden.bemerkung() : "");
            }

            FormLayout form = new FormLayout();
            form.add(mitglied, spendenart, verein, datum, betrag, bemerkung);
            form.setColspan(bemerkung, 2);
            add(form);

            Button speichern = new Button("Speichern", e -> speichern());
            speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        }

        /**
         * Validiert die Eingaben und legt die Spende an bzw. speichert sie.
         */
        private void speichern() {
            if (mitglied.getValue() == null || spendenart.getValue() == null
                    || verein.getValue() == null || datum.getValue() == null || betrag.getValue() == null) {
                UiUtil.fehler(new IllegalStateException("Bitte Mitglied, Spendenart, Träger, Datum und Betrag angeben"));
                return;
            }
            try {
                String bemerkungWert = bemerkung.getValue().isBlank() ? null : bemerkung.getValue().trim();
                if (vorhanden == null) {
                    spendeService.anlegen(mitglied.getValue().id(), spendenart.getValue(), verein.getValue(),
                            datum.getValue(), betrag.getValue(), bemerkungWert);
                    UiUtil.erfolg("Spende angelegt");
                } else {
                    spendeService.aendern(vorhanden.id(), mitglied.getValue().id(), spendenart.getValue(),
                            verein.getValue(), datum.getValue(), betrag.getValue(), bemerkungWert);
                    UiUtil.erfolg("Spende gespeichert");
                }
                grid.getDataProvider().refreshAll();
                close();
            } catch (Exception e) {
                UiUtil.fehler(e);
            }
        }
    }
}
