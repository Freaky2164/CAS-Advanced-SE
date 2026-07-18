package de.frauenhaus.ui.benutzer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.security.AppUser;
import de.frauenhaus.security.AppUserService;
import de.frauenhaus.security.AppUserService.AppUserResponse;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.UiUtil;
import jakarta.annotation.security.RolesAllowed;

/**
 * @author Nils
 *
 * Benutzerverwaltung (nur Rolle ADMIN): Anlegen, Rolle/Aktiv-Status ändern
 * und Passwort zurücksetzen.
 */
@Route(value = "benutzer", layout = MainLayout.class)
@PageTitle("Benutzer | Frauenhaus Verwaltung")
@RolesAllowed("ADMIN")
public class BenutzerView extends VerticalLayout {

    private final transient AppUserService appUserService;
    private final Grid<AppUserResponse> grid = new Grid<>();

    public BenutzerView(AppUserService appUserService) {
        this.appUserService = appUserService;

        Button neu = new Button("Neu", e -> new AnlegenDialog().open());
        neu.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button bearbeiten = new Button("Bearbeiten", e -> auswahl().ifPresent(u -> new BearbeitenDialog(u).open()));
        Button passwort = new Button("Passwort zurücksetzen", e -> auswahl().ifPresent(u -> new PasswortDialog(u).open()));

        grid.addColumn(AppUserResponse::id).setHeader("Nr.").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(AppUserResponse::username).setHeader("Benutzername").setAutoWidth(true);
        grid.addColumn(u -> u.role().name()).setHeader("Rolle").setAutoWidth(true);
        grid.addColumn(u -> u.enabled() ? "aktiv" : "deaktiviert").setHeader("Status").setAutoWidth(true);
        grid.addColumn(u -> UiUtil.zeitpunkt(u.createdAt())).setHeader("Angelegt am");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        setSizeFull();
        add(new HorizontalLayout(neu, bearbeiten, passwort), grid);
        aktualisieren();
    }

    private java.util.Optional<AppUserResponse> auswahl() {
        java.util.Optional<AppUserResponse> auswahl = grid.asSingleSelect().getOptionalValue();
        if (auswahl.isEmpty()) {
            UiUtil.fehler(new IllegalStateException("Bitte zuerst einen Benutzer auswählen"));
        }
        return auswahl;
    }

    private void aktualisieren() {
        grid.setItems(appUserService.alle());
    }

    /**
     * @author Nils
     *
     * Dialog zum Anlegen eines Benutzers mit Startpasswort und Rolle.
     */
    private final class AnlegenDialog extends Dialog {

        private AnlegenDialog() {
            setHeaderTitle("Benutzer anlegen");

            TextField username = new TextField("Benutzername");
            PasswordField passwort = new PasswordField("Passwort");
            ComboBox<AppUser.Role> rolle = new ComboBox<>("Rolle");
            rolle.setItems(AppUser.Role.values());
            rolle.setValue(AppUser.Role.SACHBEARBEITUNG);

            FormLayout form = new FormLayout(username, passwort, rolle);
            add(form);

            Button speichern = new Button("Anlegen", e -> {
                if (username.getValue().isBlank() || passwort.getValue().isBlank() || rolle.getValue() == null) {
                    UiUtil.fehler(new IllegalStateException("Bitte Benutzername, Passwort und Rolle angeben"));
                    return;
                }
                try {
                    appUserService.anlegen(username.getValue().trim(), passwort.getValue(), rolle.getValue());
                    aktualisieren();
                    UiUtil.erfolg("Benutzer angelegt");
                    close();
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            });
            speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        }
    }

    /**
     * @author Nils
     *
     * Dialog zum Ändern von Rolle und Aktiv-Status.
     */
    private final class BearbeitenDialog extends Dialog {

        private BearbeitenDialog(AppUserResponse benutzer) {
            setHeaderTitle("Benutzer bearbeiten: " + benutzer.username());

            ComboBox<AppUser.Role> rolle = new ComboBox<>("Rolle");
            rolle.setItems(AppUser.Role.values());
            rolle.setValue(benutzer.role());
            Checkbox aktiv = new Checkbox("Aktiv");
            aktiv.setValue(benutzer.enabled());

            add(new FormLayout(rolle, aktiv));

            Button speichern = new Button("Speichern", e -> {
                try {
                    appUserService.aendern(benutzer.id(), rolle.getValue(), aktiv.getValue());
                    aktualisieren();
                    UiUtil.erfolg("Benutzer gespeichert");
                    close();
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            });
            speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        }
    }

    /**
     * @author Nils
     *
     * Dialog zum Zurücksetzen des Passworts durch Administratoren.
     */
    private final class PasswortDialog extends Dialog {

        private PasswortDialog(AppUserResponse benutzer) {
            setHeaderTitle("Passwort zurücksetzen: " + benutzer.username());

            PasswordField neuesPasswort = new PasswordField("Neues Passwort");
            add(new FormLayout(neuesPasswort));

            Button speichern = new Button("Zurücksetzen", e -> {
                if (neuesPasswort.getValue().isBlank()) {
                    UiUtil.fehler(new IllegalStateException("Bitte das neue Passwort eingeben"));
                    return;
                }
                try {
                    appUserService.passwortZuruecksetzen(benutzer.id(), neuesPasswort.getValue());
                    UiUtil.erfolg("Passwort zurückgesetzt");
                    close();
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            });
            speichern.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(new Button("Abbrechen", e -> close()), speichern);
        }
    }
}
