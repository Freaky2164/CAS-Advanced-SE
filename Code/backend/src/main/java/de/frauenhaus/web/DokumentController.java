package de.frauenhaus.web;

import de.frauenhaus.service.DokumentService;
import de.frauenhaus.service.DokumentService.DokumentDownload;
import de.frauenhaus.service.DokumentService.DokumentMetadaten;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Nils
 *
 * REST-Endpunkte für Dokument-Anhänge zu den auditierbaren Stammdaten.
 */
@RestController
@RequestMapping("/api/dokumente")
public class DokumentController {

    private final DokumentService dokumentService;

    public DokumentController(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    @PostMapping(path = "/{entityTyp}/{entityId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DokumentMetadaten> hochladen(@PathVariable String entityTyp,
                                                       @PathVariable String entityId,
                                                       @RequestParam("datei") MultipartFile datei) {
        DokumentMetadaten angelegt = dokumentService.hochladen(entityTyp, entityId, datei);
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    @GetMapping("/{entityTyp}/{entityId}")
    public List<DokumentMetadaten> liste(@PathVariable String entityTyp, @PathVariable String entityId) {
        return dokumentService.liste(entityTyp, entityId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> herunterladen(@PathVariable Long id) {
        DokumentDownload dokument = dokumentService.herunterladen(id);
        return download(dokument.inhalt(), dokument.dateiname(), dokument.contentType());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(@PathVariable Long id) {
        dokumentService.loeschen(id);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<byte[]> download(byte[] inhalt, String dateiname, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dateiname + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(inhalt);
    }
}
