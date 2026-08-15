package de.frauenhaus.ui.mitglieder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import de.frauenhaus.domain.Anrede;
import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.AnredeService;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.DokumentService;
import de.frauenhaus.service.MitgliedService;
import de.frauenhaus.service.MitgliedService.MitgliedResponse;
import de.frauenhaus.service.VereinService;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.DokumenteDialog;
import de.frauenhaus.ui.support.UiUtil;
import de.frauenhaus.ui.support.VerlaufDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Pflege der Mitglieder und Adressen: durchsuchbare Liste mit Anlegen,
 * Bearbeiten, Duplizieren, Löschen sowie Dokument-Anhängen und
 * Änderungsverlauf.
 *
 * @author Paul
 */
@Route(value = "mitglieder", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Mitglieder | Frauenhaus Verwaltung")
@PermitAll
public class MitgliederView extends VerticalLayout {

    private final transient MitgliedService mitgliedService;
    private final transient AnredeService anredeService;
    private final transient VereinService vereinService;
    private final transient AuditService auditService;
    private final transient DokumentService dokumentService;

    private final TextField suche = new TextField();
    private final Grid<MitgliedResponse> grid = new Grid<>();

    /**
     * Baut die Mitglieder-Seite mit Werkzeugleiste und Liste auf.
     *
     * @param mitgliedService der Service für die Mitglieder-Pflege
     * @param anredeService der Service für die Anrede-Auswahl
     * @param vereinService der Service für die Vereinsauswahl
     * @param auditService der Service für den Änderungsverlauf
     * @param dokumentService der Service für Dokument-Anhänge
     */
    public MitgliederView(MitgliedService mitgliedService, AnredeService anredeService,
                          VereinService vereinService, AuditService auditService,
                          DokumentService dokumentService) {
        this.mitgliedService = mitgliedService;
        this.anredeService = anredeService;
        this.vereinService = vereinService;
        this.auditService = auditService;
        this.dokumentService = dokumentService;

        setSizeFull();
        add(werkzeugleiste(), grid);
        gridAufbauen();
    }

    /**
     * Baut die Werkzeugleiste mit Suche und Aktions-Buttons auf.
     */
    private HorizontalLayout werkzeugleiste() {
        suche.setPlaceholder("Suche (Name, Ort, E-Mail …)");
        suche.setClearButtonVisible(true);
        suche.setValueChangeMode(ValueChangeMode.LAZY);
        suche.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        suche.setWidth("22em");

        Button neu = new Button("Neu", e -> bearbeiten(null));
        neu.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button bearbeiten = new Button("Bearbeiten", e -> auswahl().ifPresent(this::bearbeiten));
        Button duplizieren = new Button("Duplizieren", e -> auswahl().ifPresent(this::duplizieren));
        Button loeschen = new Button("Löschen", e -> auswahl().ifPresent(this::loeschenBestaetigen));
        loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button dokumente = new Button("Dokumente", e -> auswahl().ifPresent(this::dokumenteAnzeigen));
        Button verlauf = new Button("Verlauf", e -> auswahl().ifPresent(this::verlaufAnzeigen));

        return new HorizontalLayout(suche, neu, bearbeiten, duplizieren, loeschen, dokumente, verlauf);
    }

    /**
     * Konfiguriert die Spalten und die seitenweise Datenanbindung der Liste.
     */
    private void gridAufbauen() {
        grid.addColumn(MitgliedResponse::id).setHeader("Nr.").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(MitgliedResponse::name).setHeader("Name").setAutoWidth(true);
        grid.addColumn(MitgliedResponse::vorname).setHeader("Vorname").setAutoWidth(true);
        grid.addColumn(MitgliedResponse::plz).setHeader("PLZ").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(MitgliedResponse::ort).setHeader("Ort").setAutoWidth(true);
        grid.addColumn(MitgliedResponse::email).setHeader("E-Mail").setAutoWidth(true);
        grid.addColumn(m -> m.foerderverein() ? "✓" : "").setHeader("FV").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(m -> m.frauenhaus() ? "✓" : "").setHeader("FH").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(m -> String.join(", ", m.stichworte())).setHeader("Stichworte");
        grid.setItems(query -> mitgliedService.alle(
                        PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("name", "vorname")),
                        suche.getValue())
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
    }

    /**
     * Liefert das ausgewählte Mitglied oder zeigt einen Hinweis an.
     */
    private java.util.Optional<MitgliedResponse> auswahl() {
        java.util.Optional<MitgliedResponse> auswahl = grid.asSingleSelect().getOptionalValue();
        if (auswahl.isEmpty()) {
            UiUtil.fehler(new IllegalStateException("Bitte zuerst ein Mitglied auswählen"));
        }
        return auswahl;
    }

    /**
     * Öffnet den Bearbeitungsdialog; {@code null} legt ein neues Mitglied an.
     */
    private void bearbeiten(MitgliedResponse vorhanden) {
        new MitgliedDialog(vorhanden).open();
    }

    /**
     * Dupliziert das Mitglied und aktualisiert die Liste.
     */
    private void duplizieren(MitgliedResponse m) {
        try {
            MitgliedResponse kopie = mitgliedService.duplizieren(m.id());
            grid.getDataProvider().refreshAll();
            UiUtil.erfolg("Mitglied als Nr. " + kopie.id() + " dupliziert");
        } catch (Exception e) {
            UiUtil.fehler(e);
        }
    }

    /**
     * Fragt die Löschung ab und löscht das Mitglied nach Bestätigung.
     */
    private void loeschenBestaetigen(MitgliedResponse m) {
        ConfirmDialog dialog = new ConfirmDialog("Mitglied löschen",
                "Soll das Mitglied \"" + anzeigeName(m) + "\" (Nr. " + m.id() + ") wirklich gelöscht werden?",
                "Löschen", e -> {
                    try {
                        mitgliedService.loeschen(m.id());
                        grid.getDataProvider().refreshAll();
                        UiUtil.erfolg("Mitglied gelöscht");
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
     * Öffnet den Dialog mit den Dokument-Anhängen des Mitglieds.
     */
    private void dokumenteAnzeigen(MitgliedResponse m) {
        new DokumenteDialog(dokumentService, "MITGLIED", Long.toString(m.id()), anzeigeName(m)).open();
    }

    /**
     * Öffnet den Änderungsverlauf des Mitglieds.
     */
    private void verlaufAnzeigen(MitgliedResponse m) {
        try {
            new VerlaufDialog(anzeigeName(m), auditService.verlauf(Mitglied.class, m.id())).open();
        } catch (Exception e) {
            UiUtil.fehler(e);
        }
    }

    /**
     * Liefert den Anzeigenamen aus Vorname und Name.
     */
    private static String anzeigeName(MitgliedResponse m) {
        return (m.vorname() != null ? m.vorname() + " " : "") + m.name();
    }

    /**
     * Bearbeitungsdialog für ein Mitglied; Stichworte werden als Komma-Liste
     * eingegeben, Vereine per Mehrfachauswahl.
     */
    private final class MitgliedDialog extends Dialog {

        private final transient MitgliedResponse vorhanden;

        private final ComboBox<String> anrede = new ComboBox<>("Anrede");
        private final TextField vorname = new TextField("Vorname");
        private final TextField name = new TextField("Name");
        private final TextField name2 = new TextField("Name 2");
        private final TextField name3 = new TextField("Name 3");
        private final TextField briefanrede = new TextField("Briefanrede");
        private final TextField strasse = new TextField("Straße");
        private final TextField plz = new TextField("PLZ");
        private final TextField ort = new TextField("Ort");
        private final TextField email = new TextField("E-Mail");
        private final TextField tel1 = new TextField("Telefon 1");
        private final TextField tel2 = new TextField("Telefon 2");
        private final TextField fax = new TextField("Fax");
        private final Checkbox foerderverein = new Checkbox("Förderverein");
        private final Checkbox frauenhaus = new Checkbox("Frauenhaus");
        private final TextArea bemerkung = new TextArea("Bemerkung");
        private final TextField stichworte = new TextField("Stichworte (Komma-getrennt)");
        private final MultiSelectComboBox<String> vereine = new MultiSelectComboBox<>("Vereine");

        /**
         * Baut den Dialog auf und füllt bei bestehenden Mitgliedern die Felder vor.
         *
         * @param vorhanden das zu bearbeitende Mitglied oder {@code null} zum Anlegen
         */
        private MitgliedDialog(MitgliedResponse vorhanden) {
            this.vorhanden = vorhanden;
            setHeaderTitle(vorhanden == null ? "Mitglied anlegen" : "Mitglied bearbeiten (Nr. " + vorhanden.id() + ")");
            setWidth("50em");

            anrede.setItems(anredeService.alle().stream().map(Anrede::getName).toList());
            anrede.setClearButtonVisible(true);
            vereine.setItems(vereinService.alle(null).stream().map(Verein::getName).toList());
            name.setRequiredIndicatorVisible(true);

            if (vorhanden != null) {
                anrede.setValue(vorhanden.anrede() != null ? vorhanden.anrede() : anrede.getEmptyValue());
                vorname.setValue(leerFalls(vorhanden.vorname()));
                name.setValue(leerFalls(vorhanden.name()));
                name2.setValue(leerFalls(vorhanden.name2()));
                name3.setValue(leerFalls(vorhanden.name3()));
                briefanrede.setValue(leerFalls(vorhanden.briefanrede()));
                strasse.setValue(leerFalls(vorhanden.strasse()));
                plz.setValue(leerFalls(vorhanden.plz()));
                ort.setValue(leerFalls(vorhanden.ort()));
                email.setValue(leerFalls(vorhanden.email()));
                tel1.setValue(leerFalls(vorhanden.tel1()));
                tel2.setValue(leerFalls(vorhanden.tel2()));
                fax.setValue(leerFalls(vorhanden.fax()));
                foerderverein.setValue(vorhanden.foerderverein());
                frauenhaus.setValue(vorhanden.frauenhaus());
                bemerkung.setValue(leerFalls(vorhanden.bemerkung()));
                stichworte.setValue(String.join(", ", vorhanden.stichworte()));
                vereine.setValue(new HashSet<>(vorhanden.vereine()));
            }

            FormLayout form = new FormLayout();
            form.add(anrede, vorname, name, name2, name3, briefanrede,
                    strasse, plz, ort, email, tel1, tel2, fax,
                    foerderverein, frauenhaus, stichworte, vereine, bemerkung);
            form.setColspan(bemerkung, 2);
            add(form);

            Button speichern = new Button("Speichern", e -> speichern());
            speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        }

        /**
         * Validiert die Eingaben und legt das Mitglied an bzw. speichert es.
         */
        private void speichern() {
            if (name.getValue().isBlank()) {
                UiUtil.fehler(new IllegalStateException("Name darf nicht leer sein"));
                return;
            }
            try {
                Mitglied vorlage = new Mitglied();
                vorlage.setAnrede(wertOderNull(anrede.getValue()));
                vorlage.setVorname(wertOderNull(vorname.getValue()));
                vorlage.setName(name.getValue().trim());
                vorlage.setName2(wertOderNull(name2.getValue()));
                vorlage.setName3(wertOderNull(name3.getValue()));
                vorlage.setBriefanrede(wertOderNull(briefanrede.getValue()));
                vorlage.setStrasse(wertOderNull(strasse.getValue()));
                vorlage.setPlz(wertOderNull(plz.getValue()));
                vorlage.setOrt(wertOderNull(ort.getValue()));
                vorlage.setEmail(wertOderNull(email.getValue()));
                vorlage.setTel1(wertOderNull(tel1.getValue()));
                vorlage.setTel2(wertOderNull(tel2.getValue()));
                vorlage.setFax(wertOderNull(fax.getValue()));
                vorlage.setFoerderverein(foerderverein.getValue());
                vorlage.setFrauenhaus(frauenhaus.getValue());
                vorlage.setBemerkung(wertOderNull(bemerkung.getValue()));

                List<String> stichwortNamen = UiUtil.kommaListe(stichworte.getValue());
                List<String> vereinNamen = new ArrayList<>(vereine.getValue());

                if (vorhanden == null) {
                    mitgliedService.anlegen(vorlage, stichwortNamen, vereinNamen);
                    UiUtil.erfolg("Mitglied angelegt");
                } else {
                    mitgliedService.aendern(vorhanden.id(), vorlage, stichwortNamen, vereinNamen);
                    UiUtil.erfolg("Mitglied gespeichert");
                }
                grid.getDataProvider().refreshAll();
                close();
            } catch (Exception e) {
                UiUtil.fehler(e);
            }
        }
    }

    /**
     * Liefert einen leeren String, wenn der Wert {@code null} ist.
     */
    private static String leerFalls(String wert) {
        return wert == null ? "" : wert;
    }

    /**
     * Trimmt den Wert und liefert {@code null}, wenn er leer ist.
     */
    private static String wertOderNull(String wert) {
        return wert == null || wert.isBlank() ? null : wert.trim();
    }
}
