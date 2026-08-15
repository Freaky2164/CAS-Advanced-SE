package de.frauenhaus.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Anmeldeseite des Vaadin-UIs; das Formular postet auf den von Spring Security
 * bereitgestellten "login"-Endpunkt (siehe SecurityConfig#uiFilterChain).
 *
 * @author Paul
 */
@Route("login")
@PageTitle("Anmeldung | Frauenhaus Verwaltung")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    /**
     * Baut die Anmeldeseite mit Titel und deutschsprachigem Formular auf.
     */
    public LoginView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);

        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);
        login.setI18n(deutscheTexte());

        add(new H1("Frauenhaus Verwaltung"), login);
    }

    /**
     * Zeigt die Fehlermeldung, wenn Spring Security nach fehlgeschlagener
     * Anmeldung auf /login?error umleitet.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }

    /**
     * Liefert die deutschsprachigen Texte des Anmeldeformulars.
     */
    private static LoginI18n deutscheTexte() {
        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form form = i18n.getForm();
        form.setTitle("Anmeldung");
        form.setUsername("Benutzername");
        form.setPassword("Passwort");
        form.setSubmit("Anmelden");
        LoginI18n.ErrorMessage fehler = i18n.getErrorMessage();
        fehler.setTitle("Anmeldung fehlgeschlagen");
        fehler.setMessage("Benutzername oder Passwort ist falsch – bitte erneut versuchen.");
        return i18n;
    }
}
