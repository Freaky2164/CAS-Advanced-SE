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
 * @author Nils
 *
 * Pflege der Nachschlage-Stammdaten in Reitern: Anreden, Spendentypen,
 * Bußgeldstatus, Vereine (Träger), Gerichte und Spendenarten
 * (Vaadin-Ersatz für die frühere Angular-Verwaltungs-Komponente).
 */
@Route(value = "verwaltung", layout = MainLayout.class)
@PageTitle("Verwaltung | Frauenhaus Verwaltung")
@PermitAll
public class VerwaltungView extends VerticalLayout {

    private final transient AuditService auditService;

    public VerwaltungView(AnredeService anredeService, SpendentypService spendentypService,
                          BussgeldstatusService bussgeldstatusService, VereinService vereinService,
                          GerichtService gerichtService, SpendenartService spendenartService,
                          AuditService auditService) {
        this.auditService = auditService;

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.add("Anreden", namensListe("Anrede", Anrede::getName,
                anredeService::alle, anredeService::anlegen, anredeService::loeschen));
        tabs.add("Spendentypen", namensListe("Spendentyp", Spendentyp::getName,
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
     * @author Nils
     *
     * Einfache Namensliste (Anrede, Spendentyp, Bußgeldstatus): Grid + Anlegen + Löschen.
     */
    private <T> VerticalLayout namensListe(String bezeichnung, Function<T, String> nameVon,
                                           Supplier<java.util.List<T>> laden,
                                           Consumer<String> anlegen, Consumer<String> loeschen) {
        Grid<T> grid = new Grid<>();
        grid.addColumn(nameVon::apply).setHeader(bezeichnung);
        grid.setItems(laden.get());

        TextField neuName = new TextField();
        neuName.setPlaceholder("Neue(r) " + bezeichnung);
        Button anlegenButton = new Button("Anlegen", e -> {
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

        Button loeschenButton = new Button("Löschen", e ->
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

    private static void bestaetigtLoeschen(String bezeichnung, String name, Runnable aktion) {
        ConfirmDialog dialog = new ConfirmDialog(bezeichnung + " löschen",
                "Soll \"" + name + "\" wirklich gelöscht werden?",
                "Löschen", e -> {
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
     * @author Nils
     *
     * Vereine (Träger): Name ist fachlicher Schlüssel, nur die Bezeichnung ist änderbar.
     */
    private final class VereineTab extends VerticalLayout {

        private final Grid<Verein> grid = new Grid<>();

        private VereineTab(VereinService service) {
            grid.addColumn(Verein::getName).setHeader("Name").setAutoWidth(true);
            grid.addColumn(Verein::getBezeichnung).setHeader("Bezeichnung");
            grid.setItems(service.alle(null));

            TextField name = new TextField();
            name.setPlaceholder("Name (Kürzel)");
            TextField bezeichnung = new TextField();
            bezeichnung.setPlaceholder("Bezeichnung");
            Button anlegen = new Button("Anlegen", e -> {
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

            Button loeschen = new Button("Löschen", e -> auswahl().ifPresent(v ->
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

        private java.util.Optional<Verein> auswahl() {
            java.util.Optional<Verein> auswahl = grid.asSingleSelect().getOptionalValue();
            if (auswahl.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte zuerst einen Verein auswählen"));
            }
            return auswahl;
        }
    }

    /**
     * @author Nils
     *
     * Gerichte mit Adresse: Anlegen, Ändern (übernimmt Werte in die Felder), Löschen, Verlauf.
     */
    private final class GerichteTab extends VerticalLayout {

        private final Grid<Gericht> grid = new Grid<>();

        private GerichteTab(GerichtService service) {
            grid.addColumn(Gericht::getBezeichnung).setHeader("Bezeichnung").setAutoWidth(true);
            grid.addColumn(Gericht::getStrasse).setHeader("Straße").setAutoWidth(true);
            grid.addColumn(Gericht::getPlz).setHeader("PLZ").setAutoWidth(true);
            grid.addColumn(Gericht::getOrt).setHeader("Ort").setAutoWidth(true);
            grid.setItems(service.alle(null));

            TextField bezeichnung = new TextField();
            bezeichnung.setPlaceholder("Bezeichnung");
            TextField strasse = new TextField();
            strasse.setPlaceholder("Straße");
            TextField plz = new TextField();
            plz.setPlaceholder("PLZ");
            plz.setWidth("6em");
            TextField ort = new TextField();
            ort.setPlaceholder("Ort");

            grid.asSingleSelect().addValueChangeListener(e -> {
                Gericht g = e.getValue();
                bezeichnung.setValue(g != null && g.getBezeichnung() != null ? g.getBezeichnung() : "");
                strasse.setValue(g != null && g.getStrasse() != null ? g.getStrasse() : "");
                plz.setValue(g != null && g.getPlz() != null ? g.getPlz() : "");
                ort.setValue(g != null && g.getOrt() != null ? g.getOrt() : "");
            });

            Button anlegen = new Button("Anlegen", e -> {
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
            });
            anlegen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button aendern = new Button("Ändern", e -> auswahl().ifPresent(g -> {
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
            }));

            Button loeschen = new Button("Löschen", e -> auswahl().ifPresent(g ->
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

        private java.util.Optional<Gericht> auswahl() {
            java.util.Optional<Gericht> auswahl = grid.asSingleSelect().getOptionalValue();
            if (auswahl.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte zuerst ein Gericht auswählen"));
            }
            return auswahl;
        }

        private static String wertOderNull(TextField feld) {
            return feld.getValue().isBlank() ? null : feld.getValue().trim();
        }
    }

    /**
     * @author Nils
     *
     * Spendenarten mit zugeordnetem Spendentyp.
     */
    private static final class SpendenartenTab extends VerticalLayout {

        private final Grid<Spendenart> grid = new Grid<>();

        private SpendenartenTab(SpendenartService service, SpendentypService spendentypService) {
            grid.addColumn(Spendenart::getName).setHeader("Spendenart").setAutoWidth(true);
            grid.addColumn(Spendenart::getSpendentyp).setHeader("Spendentyp");
            grid.setItems(service.alle());

            TextField name = new TextField();
            name.setPlaceholder("Spendenart");
            ComboBox<String> spendentyp = new ComboBox<>();
            spendentyp.setPlaceholder("Spendentyp");
            spendentyp.setItems(spendentypService.alle().stream().map(Spendentyp::getName).toList());

            Button anlegen = new Button("Anlegen", e -> {
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

            Button loeschen = new Button("Löschen", e -> auswahl().ifPresent(sa ->
                    bestaetigtLoeschen("Spendenart", sa.getName(), () -> {
                        service.loeschen(sa.getName());
                        grid.setItems(service.alle());
                    })));
            loeschen.addThemeVariants(ButtonVariant.LUMO_ERROR);

            add(new HorizontalLayout(name, spendentyp, anlegen, aendern, loeschen), grid);
            setSizeFull();
        }

        private java.util.Optional<Spendenart> auswahl() {
            java.util.Optional<Spendenart> auswahl = grid.asSingleSelect().getOptionalValue();
            if (auswahl.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte zuerst eine Spendenart auswählen"));
            }
            return auswahl;
        }
    }
}
