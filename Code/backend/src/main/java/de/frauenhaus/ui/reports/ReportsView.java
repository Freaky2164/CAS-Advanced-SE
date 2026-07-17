package de.frauenhaus.ui.reports;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
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
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * @author Nils
 *
 * Report-Seite: Excel-Übersichten (Bußgelder, Spenden), Serienbriefe,
 * Stichwortsuche mit Vorschau sowie der E-Mail-Verteiler-Versand
 * (Vaadin-Ersatz für die frühere Angular-Reports-Komponente).
 */
@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Reports | Frauenhaus Verwaltung")
@PermitAll
public class ReportsView extends VerticalLayout {

    private final transient BussgeldReportService bussgeldReports;
    private final transient SpendenService spendenService;
    private final transient VerteilerService verteilerService;
    private final transient StichwortsucheService stichwortsucheService;
    private final transient VereinService vereinService;

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

    private List<String> vereinsNamen() {
        return vereinService.alle(null).stream().map(Verein::getName).toList();
    }

    private static DatePicker datumsfeld(String label, LocalDate startwert) {
        DatePicker feld = new DatePicker(label);
        feld.setLocale(Locale.GERMANY);
        feld.setValue(startwert);
        return feld;
    }

    private VerticalLayout bussgeldUebersicht() {
        DatePicker von = datumsfeld("Von", LocalDate.now().withDayOfYear(1));
        DatePicker bis = datumsfeld("Bis", LocalDate.now());
        var download = UiUtil.downloadLink("Als Excel herunterladen", () -> {
            if (von.getValue() == null || bis.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte Zeitraum angeben");
            }
            return new UiUtil.Datei("bussgeld-uebersicht.xlsx", UiUtil.XLSX,
                    bussgeldReports.uebersicht(von.getValue(), bis.getValue()));
        });
        return abschnitt("Bußgeld-Übersicht (Summen je Gericht und Träger)",
                zeile(von, bis, download));
    }

    private VerticalLayout bussgeldDetail() {
        DatePicker von = datumsfeld("Von", LocalDate.now().withDayOfYear(1));
        DatePicker bis = datumsfeld("Bis", LocalDate.now());
        ComboBox<String> verein = new ComboBox<>("Träger");
        verein.setItems(vereinsNamen());
        var download = UiUtil.downloadLink("Als Excel herunterladen", () -> {
            if (von.getValue() == null || bis.getValue() == null || verein.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte Zeitraum und Träger angeben");
            }
            return new UiUtil.Datei("bussgeld-detail.xlsx", UiUtil.XLSX,
                    bussgeldReports.detail(von.getValue(), bis.getValue(), verein.getValue()));
        });
        return abschnitt("Bußgeld-Detailliste (mit Zahlungseingängen)",
                zeile(von, bis, verein, download));
    }

    private VerticalLayout spendenUebersicht() {
        IntegerField jahr = new IntegerField("Jahr");
        jahr.setValue(LocalDate.now().getYear());
        jahr.setStepButtonsVisible(true);
        var download = UiUtil.downloadLink("Als Excel herunterladen", () -> {
            if (jahr.getValue() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte das Jahr angeben");
            }
            return new UiUtil.Datei("spenden-uebersicht-" + jahr.getValue() + ".xlsx", UiUtil.XLSX,
                    spendenService.uebersicht(jahr.getValue()));
        });
        return abschnitt("Spenden-Übersicht (je Träger, Spendentyp und -art)",
                zeile(jahr, download));
    }

    private VerticalLayout serienbriefe() {
        TextField stichworte = new TextField("Stichworte (Komma-getrennt)");
        stichworte.setWidth("26em");
        ComboBox<String> verein = new ComboBox<>("Träger (für Anschreiben)");
        verein.setItems(vereinsNamen());
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
                    verteilerService.serienbrief(liste, verein.getValue()));
        });
        return abschnitt("Serienbriefe (nach Verteiler-Stichworten)",
                zeile(stichworte, verein, adressen, briefe));
    }

    private VerticalLayout stichwortsuche() {
        TextField stichworte = new TextField("Stichworte (Komma-getrennt)");
        stichworte.setWidth("26em");
        Checkbox foerderverein = new Checkbox("Nur Förderverein");
        Checkbox frauenhaus = new Checkbox("Nur Frauenhaus");

        Grid<MitgliedResponse> ergebnis = new Grid<>();
        ergebnis.addColumn(MitgliedResponse::name).setHeader("Name").setAutoWidth(true);
        ergebnis.addColumn(MitgliedResponse::vorname).setHeader("Vorname").setAutoWidth(true);
        ergebnis.addColumn(MitgliedResponse::ort).setHeader("Ort").setAutoWidth(true);
        ergebnis.addColumn(MitgliedResponse::email).setHeader("E-Mail").setAutoWidth(true);
        ergebnis.addColumn(m -> String.join(", ", m.stichworte())).setHeader("Stichworte");
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

        var download = UiUtil.downloadLink("Als Excel herunterladen", () -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "Bitte mindestens ein Stichwort angeben");
            }
            return new UiUtil.Datei("stichwortsuche.xlsx", UiUtil.XLSX,
                    stichwortsucheService.suchenAlsExcel(liste, foerderverein.getValue(), frauenhaus.getValue()));
        });

        VerticalLayout abschnitt = abschnitt("Stichwortsuche (Mitglieder nach Verteiler-Stichworten)",
                zeile(stichworte, foerderverein, frauenhaus, suchen, download));
        abschnitt.add(ergebnis);
        return abschnitt;
    }

    private VerticalLayout verteiler() {
        TextField stichworte = new TextField("Stichworte (Komma-getrennt)");
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

        ComboBox<String> traeger = new ComboBox<>("Träger (Absender)");
        traeger.setItems(vereinsNamen());
        TextField betreff = new TextField("Betreff");
        betreff.setWidth("26em");
        TextArea text = new TextArea("Text");
        text.setWidthFull();
        text.setMinHeight("8em");

        Button versenden = new Button("Versenden", e -> {
            List<String> liste = UiUtil.kommaListe(stichworte.getValue());
            if (liste.isEmpty() || traeger.getValue() == null
                    || betreff.getValue().isBlank() || text.getValue().isBlank()) {
                UiUtil.fehler(new IllegalStateException("Bitte Stichworte, Träger, Betreff und Text angeben"));
                return;
            }
            ConfirmDialog dialog = new ConfirmDialog("Verteiler versenden",
                    "Die E-Mail wird per BCC an alle Empfänger der Stichworte " + liste + " gesendet. Fortfahren?",
                    "Versenden", ev -> {
                        try {
                            VerteilerService.VersandErgebnis erg = verteilerService.versenden(
                                    liste, traeger.getValue(), betreff.getValue().trim(), text.getValue());
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

        VerticalLayout abschnitt = abschnitt("E-Mail-Verteiler",
                zeile(stichworte, anzeigen));
        abschnitt.add(empfaenger, zeile(traeger, betreff), text, versenden);
        return abschnitt;
    }

    private static VerticalLayout abschnitt(String titel, HorizontalLayout inhalt) {
        VerticalLayout layout = new VerticalLayout(new H3(titel), inhalt);
        layout.setPadding(false);
        return layout;
    }

    private static HorizontalLayout zeile(com.vaadin.flow.component.Component... komponenten) {
        HorizontalLayout zeile = new HorizontalLayout(komponenten);
        zeile.setAlignItems(Alignment.END);
        return zeile;
    }
}
