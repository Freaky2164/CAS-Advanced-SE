package de.frauenhaus.ui.reports;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.BussgeldReportService;
import de.frauenhaus.service.MitgliedService.MitgliedResponse;
import de.frauenhaus.service.SpendenService;
import de.frauenhaus.service.StichwortsucheService;
import de.frauenhaus.service.VerteilerService;
import de.frauenhaus.service.VereinService;
import de.frauenhaus.ui.MainLayout;
import de.frauenhaus.ui.support.UiUtil;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Report-Seite: Excel-Übersichten für Bußgelder und Spenden, Serienbriefe,
 * Stichwortsuche mit Vorschau sowie der E-Mail-Verteiler-Versand.
 *
 * @author Robin
 */
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Reports | Frauenhaus Verwaltung")
@PermitAll
public class ReportsView extends VerticalLayout {

    private static final String EXCEL_DOWNLOAD = "Als Excel herunterladen";
    private static final String STICHWORTE_LABEL = "Stichworte (Komma-getrennt)";

    private final transient BussgeldReportService bussgeldReports;
    private final transient SpendenService spendenService;
    private final transient VerteilerService verteilerService;
    private final transient StichwortsucheService stichwortsucheService;
    private final transient VereinService vereinService;

    /**
     * Baut die Report-Seite mit allen Report-Abschnitten auf.
     *
     * @param bussgeldReports der Service für Bußgeld-Reports
     * @param spendenService der Service für die Spendenübersicht
     * @param verteilerService der Service für Verteiler und Serienbriefe
     * @param stichwortsucheService der Service für die Stichwortsuche
     * @param vereinService der Service für die Trägerauswahl
     */
    public ReportsView(BussgeldReportService bussgeldReports, SpendenService spendenService,
                       VerteilerService verteilerService, StichwortsucheService stichwortsucheService,
                       VereinService vereinService) {
        this.bussgeldReports = bussgeldReports;
        this.spendenService = spendenService;
        this.verteilerService = verteilerService;
        this.stichwortsucheService = stichwortsucheService;
        this.vereinService = vereinService;

        add(bussgeldUebersicht(), bussgeldDetail(), spendenUebersicht(),
                serienbriefe(), stichwortsuche(), verteiler());
    }

    /**
     * Liefert die Kurznamen aller Träger für Auswahlfelder.
     */
    private List<String> vereinsNamen() {
        return vereinService.alle(null).stream().map(Verein::getName).toList();
    }

    /**
     * Erzeugt ein deutschsprachiges Datumsfeld mit Startwert.
     */
    private static DatePicker datumsfeld(String label, LocalDate startwert) {
        DatePicker feld = new DatePicker(label);
        feld.setLocale(Locale.GERMANY);
        feld.setValue(startwert);
        return feld;
    }

    /**
     * Liefert das heutige Datum in der Systemzeitzone.
     */
    private static LocalDate heute() {
        return LocalDate.now(ZoneId.systemDefault());
    }

    /**
     * Baut den Abschnitt für die Bußgeld-Übersicht als Excel-Download.
     */
    private Card bussgeldUebersicht() {
        DatePicker von = datumsfeld("Von", heute().withDayOfYear(1));
        DatePicker bis = datumsfeld("Bis", heute());
        var download = UiUtil.downloadLink(EXCEL_DOWNLOAD, () -> {
            if (von.getValue() == null || bis.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte Zeitraum angeben");
            }
            return new UiUtil.Datei("bussgeld-uebersicht.xlsx", UiUtil.XLSX,
                    bussgeldReports.uebersicht(von.getValue(), bis.getValue()));
        });
        return abschnitt("Bußgeld-Übersicht (Summen je Gericht und Träger)",
                zeile(von, bis, download));
    }

    /**
     * Baut den Abschnitt für die Bußgeld-Detailliste als Excel-Download.
     */
    private Card bussgeldDetail() {
        DatePicker von = datumsfeld("Von", heute().withDayOfYear(1));
        DatePicker bis = datumsfeld("Bis", heute());
        ComboBox<String> verein = new ComboBox<>("Träger");
        verein.setItems(vereinsNamen());
        var download = UiUtil.downloadLink(EXCEL_DOWNLOAD, () -> {
            if (von.getValue() == null || bis.getValue() == null || verein.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte Zeitraum und Träger angeben");
            }
            return new UiUtil.Datei("bussgeld-detail.xlsx", UiUtil.XLSX,
                    bussgeldReports.detail(von.getValue(), bis.getValue(), verein.getValue()));
        });
        return abschnitt("Bußgeld-Detailliste (mit Zahlungseingängen)",
                zeile(von, bis, verein, download));
    }

    /**
     * Baut den Abschnitt für die Spenden-Jahresübersicht als Excel-Download.
     */
    private Card spendenUebersicht() {
        IntegerField jahr = new IntegerField("Jahr");
        jahr.setValue(heute().getYear());
        jahr.setStepButtonsVisible(true);
        var download = UiUtil.downloadLink(EXCEL_DOWNLOAD, () -> {
            if (jahr.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte das Jahr angeben");
            }
            return new UiUtil.Datei("spenden-uebersicht-" + jahr.getValue() + ".xlsx", UiUtil.XLSX,
                    spendenService.uebersicht(jahr.getValue()));
        });
        return abschnitt("Spenden-Übersicht (je Träger, Spendentyp und -art)",
                zeile(jahr, download));
    }

    /**
     * Baut den Abschnitt für Serienbrief-Adressliste und Serienbrief-Download.
     */
    private Card serienbriefe() {
        TextField stichworte = new TextField(STICHWORTE_LABEL);
        stichworte.setWidth("26em");
        ComboBox<String> verein = new ComboBox<>("Träger (für Anschreiben)");
        verein.setItems(vereinsNamen());
        TextArea text = new TextArea("Brieftext");
        text.setPlaceholder("Wird in jedes Anschreiben zwischen Briefanrede und Grußformel eingesetzt; "
                + "leer lassen, um den Text später in Word zu ergänzen.");
        text.setWidthFull();
        text.setMinHeight("8em");
        var adressen = UiUtil.downloadLink("Adressliste (Excel)", () -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte mindestens ein Stichwort angeben");
            }
            return new UiUtil.Datei("serienbrief-adressen.xlsx", UiUtil.XLSX, verteilerService.adressen(liste));
        });
        var briefe = UiUtil.downloadLink("Serienbriefe (Word)", () -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty() || verein.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte Stichworte und Träger angeben");
            }
            return new UiUtil.Datei("serienbrief.docx", UiUtil.DOCX,
                    verteilerService.serienbrief(liste, verein.getValue(), text.getValue()));
        });
        Card abschnitt = abschnitt("Serienbriefe (nach Verteiler-Stichworten)",
                zeile(stichworte, verein, adressen, briefe));
        abschnitt.add(text);
        return abschnitt;
    }

    /**
     * Baut den Abschnitt für die Stichwortsuche mit Vorschau und Excel-Download.
     */
    private Card stichwortsuche() {
        TextField stichworte = new TextField(STICHWORTE_LABEL);
        stichworte.setWidth("26em");
        Checkbox foerderverein = new Checkbox("Nur Förderverein");
        Checkbox frauenhaus = new Checkbox("Nur Frauenhaus");

        Grid<MitgliedResponse> ergebnis = new Grid<>();
        ergebnis.addColumn(MitgliedResponse::name).setHeader("Name").setAutoWidth(true);
        ergebnis.addColumn(MitgliedResponse::vorname).setHeader("Vorname").setAutoWidth(true);
        ergebnis.addColumn(MitgliedResponse::ort).setHeader("Ort").setAutoWidth(true);
        ergebnis.addColumn(MitgliedResponse::email).setHeader("E-Mail").setAutoWidth(true);
        ergebnis.addColumn(m -> String.join(", ", m.stichworte())).setHeader("Stichworte");
        ergebnis.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        ergebnis.setAllRowsVisible(true);
        ergebnis.setVisible(false);

        Button suchen = new Button("Suchen", e -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte mindestens ein Stichwort angeben"));
                return;
            }
            try {
                List<MitgliedResponse> treffer = stichwortsucheService.suchenAlsResponses(
                        liste, foerderverein.getValue(), frauenhaus.getValue());
                ergebnis.setItems(treffer);
                ergebnis.setVisible(true);
                UiUtil.erfolg(treffer.size() + " Mitglieder gefunden");
            } catch (Exception ex) {
                UiUtil.fehler(ex);
            }
        });
        suchen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var download = UiUtil.downloadLink(EXCEL_DOWNLOAD, () -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte mindestens ein Stichwort angeben");
            }
            return new UiUtil.Datei("stichwortsuche.xlsx", UiUtil.XLSX,
                    stichwortsucheService.suchenAlsExcel(liste, foerderverein.getValue(), frauenhaus.getValue()));
        });

        Card abschnitt = abschnitt("Stichwortsuche (Mitglieder nach Verteiler-Stichworten)",
                zeile(stichworte, foerderverein, frauenhaus, suchen, download));
        abschnitt.add(ergebnis);
        return abschnitt;
    }

    /**
     * Baut den Abschnitt für den E-Mail-Verteiler-Versand.
     */
    private Card verteiler() {
        TextField stichworte = new TextField(STICHWORTE_LABEL);
        stichworte.setWidth("26em");
        TextArea empfaenger = new TextArea("Empfänger");
        empfaenger.setReadOnly(true);
        empfaenger.setWidthFull();
        empfaenger.setVisible(false);

        Button anzeigen = new Button("Empfänger anzeigen", e -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty()) {
                UiUtil.fehler(new IllegalStateException("Bitte mindestens ein Stichwort angeben"));
                return;
            }
            try {
                List<String> emails = verteilerService.emails(liste);
                empfaenger.setValue(String.join("\n", emails));
                empfaenger.setVisible(true);
                UiUtil.erfolg(emails.size() + " Empfänger gefunden");
            } catch (Exception ex) {
                UiUtil.fehler(ex);
            }
        });

        TextField betreff = new TextField("Betreff");
        betreff.setWidth("26em");
        TextArea text = new TextArea("Text");
        text.setWidthFull();
        text.setMinHeight("8em");

        Button versenden = new Button("Versenden", e -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty() || betreff.getValue().isBlank() || text.getValue().isBlank()) {
                UiUtil.fehler(new IllegalStateException("Bitte Stichworte, Betreff und Text angeben"));
                return;
            }
            ConfirmDialog dialog = new ConfirmDialog("Verteiler versenden",
                    "Die E-Mail wird per BCC an alle Empfänger der Stichworte " + liste + " gesendet. Fortfahren?",
                    "Versenden", ev -> {
                        try {
                            VerteilerService.VersandErgebnis erg = verteilerService.versenden(
                                    liste, betreff.getValue().trim(), text.getValue());
                            UiUtil.erfolg("E-Mail an " + erg.empfaengerAnzahl() + " Empfänger versendet");
                        } catch (Exception ex) {
                            UiUtil.fehler(ex);
                        }
                    },
                    "Abbrechen", ev -> {
                    });
            dialog.open();
        });
        versenden.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Card abschnitt = abschnitt("E-Mail-Verteiler",
                zeile(stichworte, anzeigen));
        abschnitt.add(empfaenger, betreff, text, versenden);
        return abschnitt;
    }

    /**
     * Erzeugt eine Karte mit Titel für einen Report-Abschnitt.
     */
    private static Card abschnitt(String titel, HorizontalLayout inhalt) {
        Card card = new Card();
        card.setTitle(new Span(titel));
        card.add(inhalt);
        card.setWidthFull();
        return card;
    }

    /**
     * Ordnet die Komponenten in einer unten ausgerichteten Zeile an.
     */
    private static HorizontalLayout zeile(com.vaadin.flow.component.Component... komponenten) {
        HorizontalLayout zeile = new HorizontalLayout(komponenten);
        zeile.setAlignItems(Alignment.END);
        return zeile;
    }
}
