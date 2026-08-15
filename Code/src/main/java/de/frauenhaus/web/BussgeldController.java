package de.frauenhaus.web;

import de.frauenhaus.domain.Bussgeld;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.AuditService.VerlaufEintrag;
import de.frauenhaus.service.BussgeldService;
import de.frauenhaus.service.BussgeldService.BussgeldResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST-Endpunkte zur Pflege der Bußgelder inkl. Zahlungseingänge (Stammdaten-CRUD).
 *
 * @author Nils
 */
@RestController
@RequestMapping("/api/bussgelder")
public class BussgeldController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record BussgeldRequest(
            @NotNull Long gerichtId, @NotBlank String verein, String status, String name, String vorname,
            String aktenzeichen, @NotNull LocalDate datum, LocalDate zieldatum,
            @NotNull @PositiveOrZero BigDecimal betrag, boolean bezahlt, String bemerkung) { }

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record EingangRequest(@NotNull LocalDate datum, @NotNull BigDecimal betrag, String bemerkung) { }

    private final BussgeldService bussgeldService;
    private final AuditService auditService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public BussgeldController(BussgeldService bussgeldService, AuditService auditService) {
        this.bussgeldService = bussgeldService;
        this.auditService = auditService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public Page<BussgeldResponse> alle(Pageable pageable,
                                       @RequestParam(required = false) String suche) {
        return bussgeldService.alle(pageable, suche);
    }

    /**
     * Liefert einen Eintrag anhand seines Schlüssels.
     */
    @GetMapping("/{id}")
    public BussgeldResponse finden(@PathVariable Long id) {
        return bussgeldService.finden(id);
    }

    /**
     * Liefert den Änderungsverlauf des Eintrags.
     */
    @GetMapping("/{id}/verlauf")
    public List<VerlaufEintrag> verlauf(@PathVariable Long id) {
        return auditService.verlauf(Bussgeld.class, id);
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<BussgeldResponse> anlegen(@Valid @RequestBody BussgeldRequest r) {
        BussgeldResponse angelegt = bussgeldService.anlegen(daten(r));
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Ändert einen bestehenden Eintrag.
     */
    @PutMapping("/{id}")
    public BussgeldResponse aendern(@PathVariable Long id, @Valid @RequestBody BussgeldRequest r) {
        return bussgeldService.aendern(id, daten(r));
    }

    /**
     * Bildet den Request auf die Eingabedaten des Service ab.
     */
    private static BussgeldService.BussgeldDaten daten(BussgeldRequest r) {
        return new BussgeldService.BussgeldDaten(r.gerichtId(), r.verein(), r.status(), r.name(),
                r.vorname(), r.aktenzeichen(), r.datum(), r.zieldatum(), r.betrag(), r.bezahlt(), r.bemerkung());
    }

    /**
     * Löscht den Eintrag.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(@PathVariable Long id) {
        bussgeldService.loeschen(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fügt dem Bußgeld einen Zahlungseingang hinzu.
     */
    @PostMapping("/{id}/eingaenge")
    public ResponseEntity<BussgeldResponse> eingangHinzufuegen(@PathVariable Long id, @Valid @RequestBody EingangRequest r) {
        BussgeldResponse aktualisiert = bussgeldService.eingangHinzufuegen(id, r.datum(), r.betrag(), r.bemerkung());
        return ResponseEntity.status(HttpStatus.CREATED).body(aktualisiert);
    }

    /**
     * Entfernt einen Zahlungseingang aus dem Bußgeld.
     */
    @DeleteMapping("/{id}/eingaenge/{eingangId}")
    public BussgeldResponse eingangEntfernen(@PathVariable Long id, @PathVariable Long eingangId) {
        return bussgeldService.eingangEntfernen(id, eingangId);
    }
}
