package de.frauenhaus.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import de.frauenhaus.ui.benutzer.BenutzerView;
import de.frauenhaus.ui.bussgelder.BussgelderView;
import de.frauenhaus.ui.mitglieder.MitgliederView;
import de.frauenhaus.ui.reports.ReportsView;
import de.frauenhaus.ui.spenden.SpendenView;
import de.frauenhaus.ui.stichworte.StichworteView;
import de.frauenhaus.ui.verwaltung.VerwaltungView;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Nils
 *
 * App-Rahmen des Vaadin-UIs: Kopfzeile mit Titel, Navigations-Drawer mit den
 * Hauptbereichen (Benutzerverwaltung nur für Rolle ADMIN) und Abmelde-Knopf.
 * @PermitAll: der Rahmen selbst ist für alle angemeldeten Benutzer zugänglich,
 * die Zugriffsregeln der einzelnen Views gelten zusätzlich.
 */
@PermitAll
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menü umschalten");

        Header header = new Header(toggle);
        header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Width.FULL);
        addToNavbar(true, header);
    }

    private void addDrawerContent() {
        H1 titel = new H1("Frauenhaus Verwaltung");
        titel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        VerticalLayout inhalt = new VerticalLayout(titel, navigation());
        inhalt.setPadding(false);
        inhalt.setSpacing(false);

        addToDrawer(inhalt, abmeldeFusszeile());
    }

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

    private Footer abmeldeFusszeile() {
        String benutzer = authContext.getPrincipalName().orElse("?");
        Span angemeldet = new Span("Angemeldet als " + benutzer);
        angemeldet.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Button abmelden = new Button("Abmelden", VaadinIcon.SIGN_OUT.create(), e -> authContext.logout());
        abmelden.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout layout = new VerticalLayout(angemeldet, abmelden);
        layout.setPadding(true);
        layout.setSpacing(false);
        return new Footer(layout);
    }

    private boolean istAdmin() {
        return authContext.getAuthenticatedUser(UserDetails.class)
                .map(user -> user.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
                .orElse(false);
    }
}
