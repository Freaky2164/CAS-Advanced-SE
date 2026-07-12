package de.frauenhaus.web;

import de.frauenhaus.domain.Verein;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.AuditService.VerlaufEintrag;
import de.frauenhaus.service.VereinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Nils
 *
 * REST-Endpunkte zur Pflege der Träger (Verein-Stammdaten).
 */
@RestController
@RequestMapping("/api/vereine")
public class VereinController {

    public record VereinRequest(@NotBlank String name, @NotBlank String bezeichnung) { }
    public record BezeichnungRequest(@NotBlank String bezeichnung) { }

    private final VereinService vereinService;
    private final AuditService auditService;

    public VereinController(VereinService vereinService, AuditService auditService) {
        this.vereinService = vereinService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Verein> alle(@RequestParam(required = false) String suche) {
        return vereinService.alle(suche);
    }

    @GetMapping("/{name}")
    public Verein finden(@PathVariable String name) {
        return vereinService.finden(name);
    }

    @GetMapping("/{name}/verlauf")
    public List<VerlaufEintrag> verlauf(@PathVariable String name) {
        return auditService.verlauf(Verein.class, name);
    }

    @PostMapping
    public ResponseEntity<Verein> anlegen(@Valid @RequestBody VereinRequest request) {
        Verein angelegt = vereinService.anlegen(request.name(), request.bezeichnung());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    @PutMapping("/{name}")
    public Verein bezeichnungAendern(@PathVariable String name, @Valid @RequestBody BezeichnungRequest request) {
        return vereinService.bezeichnungAendern(name, request.bezeichnung());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        vereinService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
