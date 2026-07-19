package de.frauenhaus.web;

import de.frauenhaus.domain.Gericht;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.AuditService.VerlaufEintrag;
import de.frauenhaus.service.GerichtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Nils
 *     <p>REST-Endpunkte zur Pflege der Gerichte (Stammdaten-CRUD).
 */
@RestController
@RequestMapping("/api/gerichte")
public class GerichtController {

  public record GerichtRequest(
      @NotBlank String bezeichnung, String strasse, String plz, String ort) {}

  private final GerichtService gerichtService;
  private final AuditService auditService;

  public GerichtController(GerichtService gerichtService, AuditService auditService) {
    this.gerichtService = gerichtService;
    this.auditService = auditService;
  }

  @GetMapping
  public List<Gericht> alle(@RequestParam(required = false) String suche) {
    return gerichtService.alle(suche);
  }

  @GetMapping("/{id}")
  public Gericht finden(@PathVariable Long id) {
    return gerichtService.finden(id);
  }

  @GetMapping("/{id}/verlauf")
  public List<VerlaufEintrag> verlauf(@PathVariable Long id) {
    return auditService.verlauf(Gericht.class, id);
  }

  @PostMapping
  public ResponseEntity<Gericht> anlegen(@Valid @RequestBody GerichtRequest request) {
    Gericht angelegt =
        gerichtService.anlegen(
            request.bezeichnung(), request.strasse(), request.plz(), request.ort());
    return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
  }

  @PutMapping("/{id}")
  public Gericht aendern(@PathVariable Long id, @Valid @RequestBody GerichtRequest request) {
    return gerichtService.aendern(
        id, request.bezeichnung(), request.strasse(), request.plz(), request.ort());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> loeschen(@PathVariable Long id) {
    gerichtService.loeschen(id);
    return ResponseEntity.noContent().build();
  }
}
