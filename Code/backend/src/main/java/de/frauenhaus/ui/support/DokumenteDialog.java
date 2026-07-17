package de.frauenhaus.ui.support;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.streams.UploadHandler;
import de.frauenhaus.service.DokumentService;
import de.frauenhaus.service.DokumentService.DokumentMetadaten;

/**
 * @author Nils
 *
 * Dialog für die Dokument-Anhänge eines Datensatzes: Liste mit Download-Link
 * und Löschen sowie Upload neuer Dateien (max. 10 MB, wie im Backend geprüft).
 */
public final class DokumenteDialog extends Dialog {

    private final transient DokumentService dokumentService;
    private final String entityTyp;
    private final String entityId;
    private final Grid<DokumentMetadaten> grid = new Grid<>();

    public DokumenteDialog(DokumentService dokumentService, String entityTyp, String entityId, String titel) {
        this.dokumentService = dokumentService;
        this.entityTyp = entityTyp;
        this.entityId = entityId;

        setHeaderTitle("Dokumente: " + titel);
        setWidth("55em");

        grid.addColumn(new ComponentRenderer<>(this::downloadLink)).setHeader("Datei").setFlexGrow(1);
        grid.addColumn(d -> UiUtil.groesse(d.groesse())).setHeader("Größe").setAutoWidth(true);
        grid.addColumn(d -> UiUtil.zeitpunkt(d.hochgeladenAm())).setHeader("Hochgeladen am").setAutoWidth(true);
        grid.addColumn(DokumentMetadaten::hochgeladenVon).setHeader("Von").setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(this::loeschenButton)).setHeader("").setAutoWidth(true);
        grid.setAllRowsVisible(true);

        Upload upload = new Upload(UploadHandler.inMemory((metadata, daten) -> {
            try {
                dokumentService.hochladen(entityTyp, entityId, metadata.fileName(), metadata.contentType(), daten);
            } catch (Exception e) {
                getUI().ifPresent(ui -> ui.access(() -> UiUtil.fehler(e)));
            }
        }));
        upload.setMaxFileSize(10 * 1024 * 1024);
        upload.addAllFinishedListener(e -> aktualisieren());

        add(upload, grid);
        getFooter().add(new Button("Schließen", e -> close()));
        aktualisieren();
    }

    private com.vaadin.flow.component.html.Anchor downloadLink(DokumentMetadaten d) {
        return UiUtil.downloadLink(d.dateiname(), () -> {
            DokumentService.DokumentDownload download = dokumentService.herunterladen(d.id());
            return new UiUtil.Datei(download.dateiname(), download.contentType(), download.inhalt());
        });
    }

    private Button loeschenButton(DokumentMetadaten d) {
        Button button = new Button("Löschen", e -> {
            try {
                dokumentService.loeschen(d.id());
                aktualisieren();
            } catch (Exception ex) {
                UiUtil.fehler(ex);
            }
        });
        button.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        return button;
    }

    private void aktualisieren() {
        grid.setItems(dokumentService.liste(entityTyp, entityId));
    }
}
