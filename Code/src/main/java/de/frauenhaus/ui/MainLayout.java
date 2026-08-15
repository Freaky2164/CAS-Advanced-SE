package de.frauenhaus.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.frauenhaus.ui.benutzer.BenutzerView;
import de.frauenhaus.ui.bussgelder.BussgelderView;
import de.frauenhaus.ui.mitglieder.MitgliederView;
import de.frauenhaus.ui.reports.ReportsView;
import de.frauenhaus.ui.spenden.SpendenView;
import de.frauenhaus.ui.stichworte.StichworteView;
import de.frauenhaus.ui.verwaltung.VerwaltungView;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * App-Rahmen des Vaadin-UIs: Kopfzeile mit Titel, Navigations-Drawer mit den
 * Hauptbereichen (Benutzerverwaltung nur für Rolle ADMIN) und Abmelde-Knopf.
 * Der Rahmen selbst ist für alle angemeldeten Benutzer zugänglich, die
 * Zugriffsregeln der einzelnen Views gelten zusätzlich.
 *
 * @author Paul
 */
@PermitAll
public class MainLayout extends AppLayout {

    private static final String FONT_WEIGHT = "font-weight";

    private final transient AuthenticationContext authContext;

    /**
     * Baut den App-Rahmen mit Drawer und Kopfzeile auf.
     *
     * @param authContext der Security-Kontext des angemeldeten Benutzers
     */
    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    /**
     * Baut die Kopfzeile mit Drawer-Umschalter und Anwendungstitel auf.
     */
    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menü umschalten");

        H1 titel = new H1("Frauenhaus Verwaltung");
        titel.getStyle()
                .set("font-size", "1.15rem")
                .set(FONT_WEIGHT, "600")
                .set("margin", "0");

        addToNavbar(true, toggle, titel);
    }

    /**
     * Baut den Drawer mit Logo, Navigation und Benutzer-Fußzeile auf.
     */
    private void addDrawerContent() {
        Span logo = new Span(VaadinIcon.HOME_O.create(), new Span(" Frauenhaus"));
        logo.getStyle()
                .set("font-size", "1.1rem")
                .set(FONT_WEIGHT, "700")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.5rem")
                .set("padding", "1rem");

        VerticalLayout inhalt = new VerticalLayout(logo, navigation());
        inhalt.setPadding(false);
        inhalt.setSpacing(false);

        addToDrawer(inhalt, benutzerFusszeile());
    }

    /**
     * Baut die Seitennavigation auf; der Benutzer-Eintrag erscheint nur für
     * Administratoren.
     */
    private SideNav navigation() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Mitglieder", MitgliederView.class, VaadinIcon.USERS.create()));
        nav.addItem(new SideNavItem("Spenden", SpendenView.class, VaadinIcon.GIFT.create()));
        nav.addItem(new SideNavItem("Bußgelder", BussgelderView.class, VaadinIcon.GAVEL.create()));
        nav.addItem(new SideNavItem("Verwaltung", VerwaltungView.class, VaadinIcon.COG.create()));
        nav.addItem(new SideNavItem("Stichworte", StichworteView.class, VaadinIcon.TAGS.create()));
        nav.addItem(new SideNavItem("Reports", ReportsView.class, VaadinIcon.CHART.create()));
        if (istAdmin()) {
            nav.addItem(new SideNavItem("Benutzer", BenutzerView.class, VaadinIcon.KEY.create()));
        }
        return nav;
    }

    /**
     * Baut die Fußzeile mit Avatar, Benutzername und Abmelde-Knopf auf.
     */
    private Footer benutzerFusszeile() {
        String benutzer = authContext.getPrincipalName().orElse("?");

        Avatar avatar = new Avatar(benutzer);
        Span name = new Span(benutzer);
        name.getStyle().set(FONT_WEIGHT, "600");

        Button abmelden = new Button(VaadinIcon.SIGN_OUT.create(), e -> authContext.logout());
        abmelden.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        abmelden.setTooltipText("Abmelden");
        abmelden.setAriaLabel("Abmelden");

        HorizontalLayout zeile = new HorizontalLayout(avatar, name, abmelden);
        zeile.setAlignItems(FlexComponent.Alignment.CENTER);
        zeile.setWidthFull();
        zeile.expand(name);
        zeile.getStyle().set("padding", "0.75rem 1rem");

        return new Footer(zeile);
    }

    /**
     * Prüft, ob der angemeldete Benutzer die Rolle ADMIN besitzt.
     */
    private boolean istAdmin() {
        return authContext.getAuthenticatedUser(UserDetails.class)
                .map(user -> user.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
                .orElse(false);
    }
}
