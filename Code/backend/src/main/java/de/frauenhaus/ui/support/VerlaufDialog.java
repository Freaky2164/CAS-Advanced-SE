package de.frauenhaus.ui.support;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.frauenhaus.service.AuditService.VerlaufEintrag;

import java.util.List;
import java.util.Objects;

/**
 * Dialog mit dem Änderungsverlauf (Envers-Audit) eines Datensatzes:
 * je Revision Zeitpunkt, Benutzer, Art und die geänderten Felder alt → neu.
 *
 * @author Robin
 */
public final class VerlaufDialog extends Dialog {

    /**
     * Baut den Dialog mit der Verlaufstabelle auf.
     *
     * @param titel die Kurzbeschreibung des Datensatzes für den Dialogtitel
     * @param eintraege die anzuzeigenden Verlaufseinträge
     */
    public VerlaufDialog(String titel, List<VerlaufEintrag> eintraege) {
        setHeaderTitle("Verlauf: " + titel);
        setWidth("60em");

        Grid<VerlaufEintrag> grid = new Grid<>();
        grid.setItems(eintraege);
        grid.addColumn(e -> UiUtil.zeitpunkt(e.zeitpunkt())).setHeader("Zeitpunkt").setAutoWidth(true);
        grid.addColumn(VerlaufEintrag::benutzername).setHeader("Benutzer").setAutoWidth(true);
        grid.addColumn(VerlaufEintrag::typ).setHeader("Art").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(VerlaufDialog::aenderungen)).setHeader("Änderungen").setFlexGrow(1);
        grid.setAllRowsVisible(true);
        add(grid);

        Button schliessen = new Button("Schließen", e -> close());
        getFooter().add(schliessen);
    }

    /**
     * Stellt die Feld-Änderungen eines Eintrags zeilenweise als alt → neu dar.
     */
    private static Div aenderungen(VerlaufEintrag eintrag) {
        Div div = new Div();
        eintrag.aenderungen().forEach(a -> {
            Div zeile = new Div();
            zeile.setText(a.feld() + ": " + wert(a.alt()) + " → " + wert(a.neu()));
            div.add(zeile);
        });
        return div;
    }

    /**
     * Liefert die Textdarstellung eines Feldwerts; {@code null} wird als "–" angezeigt.
     */
    private static String wert(Object wert) {
        return Objects.toString(wert, "–");
    }
}
