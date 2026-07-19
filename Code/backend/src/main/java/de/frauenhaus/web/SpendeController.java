package de.frauenhaus.web;

import de.frauenhaus.domain.Spende;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.AuditService.VerlaufEintrag;
import de.frauenhaus.service.SpendeService;
import de.frauenhaus.service.SpendeService.SpendeResponse;
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
 * REST-Endpunkte zur Pflege der Spenden (Stammdaten-CRUD).
 *
 * @author Robin
 */
@RestController
@RequestMapping("/api/spenden")
public class SpendeController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record SpendeRequest(
            @NotNull Long mitgliedId, @NotBlank String spendenart, @NotBlank String verein,
            @NotNull LocalDate datum, @NotNull @PositiveOrZero BigDecimal betrag, String bemerkung) { }

    private final SpendeService spendeService;
    private final AuditService auditService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public SpendeController(SpendeService spendeService, AuditService auditService) {
        this.spendeService = spendeService;
        this.auditService = auditService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public Page<SpendeResponse> alle(Pageable pageable,
                                     @RequestParam(required = false) String suche) {
        return spendeService.alle(pageable, suche);
    }

    /**
     * Liefert einen Eintrag anhand seines Schlüssels.
     */
    @GetMapping("/{id}")
    public SpendeResponse finden(@PathVariable Long id) {
        return spendeService.finden(id);
    }

    /**
     * Liefert den Änderungsverlauf des Eintrags.
     */
    @GetMapping("/{id}/verlauf")
    public List<VerlaufEintrag> verlauf(@PathVariable Long id) {
        return auditService.verlauf(Spende.class, id);
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<SpendeResponse> anlegen(@Valid @RequestBody SpendeRequest request) {
        SpendeResponse angelegt = spendeService.anlegen(request.mitgliedId(), request.spendenart(),
                request.verein(), request.datum(), request.betrag(), request.bemerkung());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Ändert einen bestehenden Eintrag.
     */
    @PutMapping("/{id}")
    public SpendeResponse aendern(@PathVariable Long id, @Valid @RequestBody SpendeRequest request) {
        return spendeService.aendern(id, request.mitgliedId(), request.spendenart(),
                request.verein(), request.datum(), request.betrag(), request.bemerkung());
    }

    /**
     * Löscht den Eintrag.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(@PathVariable Long id) {
        spendeService.loeschen(id);
        return ResponseEntity.noContent().build();
    }
}
