package de.frauenhaus.ui.verwaltung;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.domain.Anrede;
import de.frauenhaus.domain.Bussgeldstatus;
import de.frauenhaus.domain.Gericht;
import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.domain.Spendentyp;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.AnredeService;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.BussgeldstatusService;
import de.frauenhaus.service.GerichtService;
import de.frauenhaus.service.SpendenartService;
import de.frauenhaus.service.SpendentypService;
import de.frauenhaus.service.VereinService;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.UiUtil;
import de.frauenhaus.ui.support.VerlaufDialog;
import jakarta.annotation.security.PermitAll;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Pflege der Nachschlage-Stammdaten in Reitern: Anreden, Spendentypen,
 * Bußgeldstatus, Vereine (Träger), Gerichte und Spendenarten.
 *
 * @author Paul
 */
@Route(value = "verwaltung", layout = MainLayout.class)
@PageTitle("Verwaltung | Frauenhaus Verwaltung")
@PermitAll
public class VerwaltungView extends VerticalLayout {

    private static final String ANLEGEN = "Anlegen";
    private static final String LOESCHEN = "Löschen";
    private static final String BEZEICHNUNG_LABEL = "Bezeichnung";
    private static final String SPENDENTYP_LABEL = "Spendentyp";
    private static final String SPENDENART_LABEL = "Spendenart";

    private final transient AuditService auditService;

    /**
     * Baut die Verwaltungs-Seite mit einem Reiter je Stammdaten-Art auf.
     *
     * @param anredeService der Service für Anreden
     * @param spendentypService der Service für Spendentypen
     * @param bussgeldstatusService der Service für Bußgeldstatus
     * @param vereinService der Service für Vereine
     * @param gerichtService der Service für Gerichte
     * @param spendenartService der Service für Spendenarten
     * @param auditService der Service für den Änderungsverlauf
     */
    public VerwaltungView(AnredeService anredeService, SpendentypService spendentypService,
                          BussgeldstatusService bussgeldstatusService, VereinService vereinService,
                          GerichtService gerichtService, SpendenartService spendenartService,
                          AuditService auditService) {
        this.auditService = auditService;

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.add("Anreden", namensListe("Anrede", Anrede::getName,
                anredeService::alle, anredeService::anlegen, anredeService::loeschen));
        tabs.add("Spendentypen", namensListe(SPENDENTYP_LABEL, Spendentyp::getName,
                spendentypService::alle, spendentypService::anlegen, spendentypService::loeschen));
        tabs.add("Bußgeldstatus", namensListe("Bußgeldstatus", Bussgeldstatus::getName,
                bussgeldstatusService::alle, bussgeldstatusService::anlegen, bussgeldstatusService::loeschen));
        tabs.add("Vereine", new VereineTab(vereinService));
        tabs.add("Gerichte", new GerichteTab(gerichtService));
        tabs.add("Spendenarten", new SpendenartenTab(spendenartService, spendentypService));

        setSizeFull();
        add(tabs);
    }

    /**
     * Baut eine einfache Namensliste (Anrede, Spendentyp, Bußgeldstatus) mit
     * Grid, Anlegen und Löschen auf.
     *
     * @param bezeichnung die Bezeichnung der Stammdaten-Art
     * @param nameVon liefert den Namen eines Eintrags
     * @param laden lädt alle Einträge
     * @param anlegen legt einen Eintrag mit dem gegebenen Namen an
     * @param loeschen löscht den Eintrag mit dem gegebenen Namen
     * @return das fertige Reiter-Layout
     */
    private <T> VerticalLayout namensListe(String bezeichnung, Function<T, String> nameVon,
                                           Supplier<java.util.List<T>> laden,
                                           Consumer<String> anlegen, Consumer<String> loeschen) {
        Grid<T> grid = new Grid<>();
        grid.addColumn(nameVon::apply).setHeader(bezeichnung);
        grid.setItems(laden.get());

        TextField neuName = new TextField();
        neuName.setPlaceholder("Neue(r) " + bezeichnung);
        Button anlegenButton = new Button(ANLEGEN, e -> {
            if (neuName.getValue().isBlank()) {
                return;
            }
            try {
                anlegen.accept(neuName.getValue().trim());
                neuName.clear();
                grid.setItems(laden.get());
                UiUtil.erfolg(bezeichnung + " angelegt");
            } catch (Exception ex) {
                UiUtil.fehler(ex);
            }
        });
        anlegenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button loeschenButton = new Button(LOESCHEN, e ->
                grid.asSingleSelect().getOptionalValue().ifPresentOrElse(auswahl ->
                                bestaetigtLoeschen(bezeichnung, nameVon.apply(auswahl), () -> {
                                    loeschen.accept(nameVon.apply(auswahl));
                                    grid.setItems(laden.get());
                                }),
                        () -> UiUtil.fehler(new IllegalStateException("Bitte zuerst einen Eintrag auswählen"))));
        loeschenButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        VerticalLayout layout = new VerticalLayout(new HorizontalLayout(neuName, anlegenButton, loeschenButton), grid);
        layout.setSizeFull();
        return layout;
    }

    /**
     * Fragt die Löschung ab und führt die Aktion nach Bestätigung aus.
     *
     * @param bezeichnung die Bezeichnung der Stammdaten-Art
     * @param name der Name des zu löschenden Eintrags
     * @param aktion die auszuführende Löschaktion
     */
    private static void bestaetigtLoeschen(String bezeichnung, String name, Runnable aktion) {
        ConfirmDialog dialog = new ConfirmDialog(bezeichnung + " löschen",
                "Soll \"" + name + "\" wirklich gelöscht werden?",
                LOESCHEN, e -> {
                    try {
                        aktion.run();
                        UiUtil.erfolg(bezeichnung + " gelöscht");
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
     * Reiter für Vereine (Träger): der Name ist fachlicher Schlüssel, nur die
     * Bezeichnung ist änderbar.
     */
    private final class VereineTab extends VerticalLayout {

        private final Grid<Verein> grid = new Grid<>();

        /**
         * Baut den Reiter mit Eingabefeldern, Buttons und Liste auf.
         *
         * @param service der Service für die Vereinspflege
         */
        private VereineTab(VereinService service) {
            grid.addColumn(Verein::getName).setHeader("Name").setAutoWidth(true);
            grid.addColumn(Verein::getBezeichnung).setHeader(BEZEICHNUNG_LABEL);
            grid.setItems(service.alle(null));

            TextField name = new TextField();
            name.setPlaceholder("Name (Kürzel)");
            TextField bezeichnung = new TextField();
            bezeichnung.setPlaceholder(BEZEICHNUNG_LABEL);
            Button anlegen = new Button(ANLEGEN, e -> {
                if (name.getValue().isBlank() || bezeichnung.getValue().isBlank()) {
                    UiUtil.fehler(new IllegalStateException("Bitte Name und Bezeichnung angeben"));
                    return;
                }
                try {
                    service.anlegen(name.getValue().trim(), bezeichnung.getValue().trim());
                    name.clear();
                    bezeichnung.clear();
                    grid.setItems(service.alle(null));
                    UiUtil.erfolg("Verein angelegt");
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            });
            anlegen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button aendern = new Button("Bezeichnung ändern", e -> auswahl().ifPresent(v -> {
                if (bezeichnung.getValue().isBlank()) {
                    UiUtil.fehler(new IllegalStateException("Bitte die neue Bezeichnung eingeben"));
                    return;
                }
                try {
                    service.bezeichnungAendern(v.getName(), bezeichnung.getValue().trim());
                    bezeichnung.clear();
                    grid.setItems(service.alle(null));
                    UiUtil.erfolg("Bezeichnung geändert");
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            }));

            Button loeschen = new Button(LOESCHEN, e -> auswahl().ifPresent(v ->
                    bestaetigtLoeschen("Verein", v.getName(), () -> {
                        service.loeschen(v.getName());
                        grid.setItems(service.alle(null));
                    })));
            loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);

            Button verlauf = new Button("Verlauf", e -> auswahl().ifPresent(v ->
                    new VerlaufDialog(v.getName(), auditService.verlauf(Verein.class, v.getName())).open()));

            add(new HorizontalLayout(name, bezeichnung, anlegen, aendern, loeschen, verlauf), grid);
            setSizeFull();
        }

        /**
         * Liefert den ausgewählten Verein oder zeigt einen Hinweis an.
         */
        private java.util.Optional<Verein> auswahl() {
            java.util.Optional<Verein> auswahl = grid.asSingleSelect().getOptionalValue();
            if (auswahl.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte zuerst einen Verein auswählen"));
            }
            return auswahl;
        }
    }

    /**
     * Reiter für Gerichte mit Adresse: Anlegen, Ändern (übernimmt Werte in die
     * Felder), Löschen und Verlauf.
     */
    private final class GerichteTab extends VerticalLayout {

        private final Grid<Gericht> grid = new Grid<>();
        private final TextField bezeichnung = new TextField();
        private final TextField strasse = new TextField();
        private final TextField plz = new TextField();
        private final TextField ort = new TextField();
        private final transient GerichtService service;

        /**
         * Baut den Reiter mit Eingabefeldern, Buttons und Liste auf.
         *
         * @param service der Service für die Gerichtspflege
         */
        private GerichteTab(GerichtService service) {
            this.service = service;
            grid.addColumn(Gericht::getBezeichnung).setHeader(BEZEICHNUNG_LABEL).setAutoWidth(true);
            grid.addColumn(Gericht::getStrasse).setHeader("Straße").setAutoWidth(true);
            grid.addColumn(Gericht::getPlz).setHeader("PLZ").setAutoWidth(true);
            grid.addColumn(Gericht::getOrt).setHeader("Ort").setAutoWidth(true);
            grid.setItems(service.alle(null));
            grid.asSingleSelect().addValueChangeListener(e -> felderUebernehmen(e.getValue()));

            bezeichnung.setPlaceholder(BEZEICHNUNG_LABEL);
            strasse.setPlaceholder("Straße");
            plz.setPlaceholder("PLZ");
            plz.setWidth("6em");
            ort.setPlaceholder("Ort");

            Button anlegen = new Button(ANLEGEN, e -> anlegen());
            anlegen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button aendern = new Button("Ändern", e -> auswahl().ifPresent(this::aendern));
            Button loeschen = new Button(LOESCHEN, e -> auswahl().ifPresent(g ->
                    bestaetigtLoeschen("Gericht", g.getBezeichnung(), () -> {
                        service.loeschen(g.getId());
                        grid.setItems(service.alle(null));
                    })));
            loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);
            Button verlauf = new Button("Verlauf", e -> auswahl().ifPresent(g ->
                    new VerlaufDialog(g.getBezeichnung(), auditService.verlauf(Gericht.class, g.getId())).open()));

            add(new HorizontalLayout(bezeichnung, strasse, plz, ort, anlegen, aendern, loeschen, verlauf), grid);
            setSizeFull();
        }

        /**
         * Übernimmt die Werte des ausgewählten Gerichts in die Eingabefelder.
         */
        private void felderUebernehmen(Gericht g) {
            bezeichnung.setValue(g != null && g.getBezeichnung() != null ? g.getBezeichnung() : "");
            strasse.setValue(g != null && g.getStrasse() != null ? g.getStrasse() : "");
            plz.setValue(g != null && g.getPlz() != null ? g.getPlz() : "");
            ort.setValue(g != null && g.getOrt() != null ? g.getOrt() : "");
        }

        /**
         * Legt ein Gericht aus den Eingabefeldern an.
         */
        private void anlegen() {
            if (bezeichnung.getValue().isBlank()) {
                UiUtil.fehler(new IllegalStateException("Bitte die Bezeichnung angeben"));
                return;
            }
            try {
                service.anlegen(bezeichnung.getValue().trim(), wertOderNull(strasse), wertOderNull(plz), wertOderNull(ort));
                grid.setItems(service.alle(null));
                UiUtil.erfolg("Gericht angelegt");
            } catch (Exception ex) {
                UiUtil.fehler(ex);
            }
        }

        /**
         * Übernimmt die Eingabefelder in das ausgewählte Gericht.
         */
        private void aendern(Gericht g) {
            if (bezeichnung.getValue().isBlank()) {
                UiUtil.fehler(new IllegalStateException("Bitte die Bezeichnung angeben"));
                return;
            }
            try {
                service.aendern(g.getId(), bezeichnung.getValue().trim(),
                        wertOderNull(strasse), wertOderNull(plz), wertOderNull(ort));
                grid.setItems(service.alle(null));
                UiUtil.erfolg("Gericht geändert");
            } catch (Exception ex) {
                UiUtil.fehler(ex);
            }
        }

        /**
         * Liefert das ausgewählte Gericht oder zeigt einen Hinweis an.
         */
        private java.util.Optional<Gericht> auswahl() {
            java.util.Optional<Gericht> auswahl = grid.asSingleSelect().getOptionalValue();
            if (auswahl.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte zuerst ein Gericht auswählen"));
            }
            return auswahl;
        }

        /**
         * Trimmt den Feldwert und liefert {@code null}, wenn er leer ist.
         */
        private static String wertOderNull(TextField feld) {
            return feld.getValue().isBlank() ? null : feld.getValue().trim();
        }
    }

    /**
     * Reiter für Spendenarten mit zugeordnetem Spendentyp.
     */
    private static final class SpendenartenTab extends VerticalLayout {

        private final Grid<Spendenart> grid = new Grid<>();

        /**
         * Baut den Reiter mit Eingabefeldern, Buttons und Liste auf.
         *
         * @param service der Service für die Spendenart-Pflege
         * @param spendentypService der Service für die Spendentyp-Auswahl
         */
        private SpendenartenTab(SpendenartService service, SpendentypService spendentypService) {
            grid.addColumn(Spendenart::getName).setHeader(SPENDENART_LABEL).setAutoWidth(true);
            grid.addColumn(Spendenart::getSpendentyp).setHeader(SPENDENTYP_LABEL);
            grid.setItems(service.alle());

            TextField name = new TextField();
            name.setPlaceholder(SPENDENART_LABEL);
            ComboBox<String> spendentyp = new ComboBox<>();
            spendentyp.setPlaceholder(SPENDENTYP_LABEL);
            spendentyp.setItems(spendentypService.alle().stream().map(Spendentyp::getName).toList());

            Button anlegen = new Button(ANLEGEN, e -> {
                if (name.getValue().isBlank() || spendentyp.getValue() == null) {
                    UiUtil.fehler(new IllegalStateException("Bitte Spendenart und Spendentyp angeben"));
                    return;
                }
                try {
                    service.anlegen(name.getValue().trim(), spendentyp.getValue());
                    name.clear();
                    grid.setItems(service.alle());
                    UiUtil.erfolg("Spendenart angelegt");
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            });
            anlegen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button aendern = new Button("Spendentyp ändern", e -> auswahl().ifPresent(sa -> {
                if (spendentyp.getValue() == null) {
                    UiUtil.fehler(new IllegalStateException("Bitte den neuen Spendentyp auswählen"));
                    return;
                }
                try {
                    service.spendentypAendern(sa.getName(), spendentyp.getValue());
                    grid.setItems(service.alle());
                    UiUtil.erfolg("Spendentyp geändert");
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            }));

            Button loeschen = new Button(LOESCHEN, e -> auswahl().ifPresent(sa ->
                    bestaetigtLoeschen(SPENDENART_LABEL, sa.getName(), () -> {
                        service.loeschen(sa.getName());
                        grid.setItems(service.alle());
                    })));
            loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);

            add(new HorizontalLayout(name, spendentyp, anlegen, aendern, loeschen), grid);
            setSizeFull();
        }

        /**
         * Liefert die ausgewählte Spendenart oder zeigt einen Hinweis an.
         */
        private java.util.Optional<Spendenart> auswahl() {
            java.util.Optional<Spendenart> auswahl = grid.asSingleSelect().getOptionalValue();
            if (auswahl.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte zuerst eine Spendenart auswählen"));
            }
            return auswahl;
        }
    }
}
