package de.frauenhaus.ui.stichworte;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.service.StichwortService;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.UiUtil;
import jakarta.annotation.security.PermitAll;

import java.util.List;

/**
 * Pflege der Verteiler-Stichworte: Zusammenstellen (alte bleiben erhalten)
 * und Zusammenfassen (alte werden gelöscht) mehrerer Stichworte zu einem neuen.
 *
 * @author Paul
 */
@Route(value = "stichworte", layout = MainLayout.class)
@PageTitle("Stichworte | Frauenhaus Verwaltung")
@PermitAll
public class StichworteView extends VerticalLayout {

    /**
     * Baut die Seite mit den Formularen für Zusammenstellen und Zusammenfassen auf.
     *
     * @param stichwortService der Service für die Stichwort-Pflege
     */
    public StichworteView(StichwortService stichwortService) {
        add(new H3("Stichworte zusammenstellen"),
                new Paragraph("Ordnet dem neuen Stichwort alle Mitglieder der alten Stichworte zu; die alten bleiben erhalten."),
                zusammenfuehrenFormular("Zusammenstellen", false, stichwortService),
                new H3("Stichworte zusammenfassen"),
                new Paragraph("Wie Zusammenstellen, löscht danach aber die alten Stichworte und ihre Zuordnungen."),
                zusammenfuehrenFormular("Zusammenfassen", true, stichwortService));
    }

    /**
     * Baut ein Formular zum Zusammenführen von Stichworten auf.
     *
     * @param aktion die Beschriftung des Aktions-Buttons
     * @param zusammenfassen wenn {@code true}, werden die alten Stichworte gelöscht
     * @param stichwortService der Service für die Stichwort-Pflege
     * @return das fertige Formular-Layout
     */
    private VerticalLayout zusammenfuehrenFormular(String aktion, boolean zusammenfassen,
                                                   StichwortService stichwortService) {
        TextField neu = new TextField("Neues Stichwort");
        neu.setWidth("20em");
        TextField alte = new TextField("Alte Stichworte (Komma-getrennt)");
        alte.setWidth("30em");

        Button ausfuehren = new Button(aktion, e -> {
            List<String> alteListe = UiUtil.kommaListe(alte.getValue());
            if (neu.getValue().isBlank() || alteListe.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte neues Stichwort und mindestens ein altes Stichwort angeben"));
                return;
            }
            Runnable arbeit = () -> {
                try {
                    int zugeordnet = zusammenfassen
                            ? stichwortService.zusammenfassen(neu.getValue().trim(), alteListe)
                            : stichwortService.zusammenstellen(neu.getValue().trim(), alteListe);
                    UiUtil.erfolg(zugeordnet + " Mitglieder dem Stichwort \"" + neu.getValue().trim() + "\" zugeordnet");
                    neu.clear();
                    alte.clear();
                } catch (Exception ex) {
                    UiUtil.fehler(ex);
                }
            };
            if (zusammenfassen) {
                ConfirmDialog dialog = new ConfirmDialog("Stichworte zusammenfassen",
                        "Die alten Stichworte " + alteListe + " werden dabei gelöscht. Fortfahren?",
                        "Zusammenfassen", ev -> arbeit.run(),
                        "Abbrechen", ev -> {
                        });
                dialog.setConfirmButtonTheme("error primary");
                dialog.open();
            } else {
                arbeit.run();
            }
        });
        ausfuehren.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(neu, alte, ausfuehren);
        layout.setPadding(false);
        return layout;
    }
}
