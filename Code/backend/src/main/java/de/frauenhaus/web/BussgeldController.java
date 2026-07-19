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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Nils
 *     <p>REST-Endpunkte zur Pflege der Bußgelder inkl. Zahlungseingänge (Stammdaten-CRUD).
 */
@RestController
@RequestMapping("/api/bussgelder")
public class BussgeldController {

  public record BussgeldRequest(
      @NotNull Long gerichtId,
      @NotBlank String verein,
      String status,
      String name,
      String vorname,
      String aktenzeichen,
      @NotNull LocalDate datum,
      LocalDate zieldatum,
      @NotNull @PositiveOrZero BigDecimal betrag,
      boolean bezahlt,
      String bemerkung) {}

  public record EingangRequest(
      @NotNull LocalDate datum, @NotNull BigDecimal betrag, String bemerkung) {}

  private final BussgeldService bussgeldService;
  private final AuditService auditService;

  public BussgeldController(BussgeldService bussgeldService, AuditService auditService) {
    this.bussgeldService = bussgeldService;
    this.auditService = auditService;
  }

  @GetMapping
  public Page<BussgeldResponse> alle(
      Pageable pageable, @RequestParam(required = false) String suche) {
    return bussgeldService.alle(pageable, suche);
  }

  @GetMapping("/{id}")
  public BussgeldResponse finden(@PathVariable Long id) {
    return bussgeldService.finden(id);
  }

  @GetMapping("/{id}/verlauf")
  public List<VerlaufEintrag> verlauf(@PathVariable Long id) {
    return auditService.verlauf(Bussgeld.class, id);
  }

  @PostMapping
  public ResponseEntity<BussgeldResponse> anlegen(@Valid @RequestBody BussgeldRequest r) {
    BussgeldResponse angelegt =
        bussgeldService.anlegen(
            r.gerichtId(),
            r.verein(),
            r.status(),
            r.name(),
            r.vorname(),
            r.aktenzeichen(),
            r.datum(),
            r.zieldatum(),
            r.betrag(),
            r.bezahlt(),
            r.bemerkung());
    return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
  }

  @PutMapping("/{id}")
  public BussgeldResponse aendern(@PathVariable Long id, @Valid @RequestBody BussgeldRequest r) {
    return bussgeldService.aendern(
        id,
        r.gerichtId(),
        r.verein(),
        r.status(),
        r.name(),
        r.vorname(),
        r.aktenzeichen(),
        r.datum(),
        r.zieldatum(),
        r.betrag(),
        r.bezahlt(),
        r.bemerkung());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> loeschen(@PathVariable Long id) {
    bussgeldService.loeschen(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * @author Nils
   *     <p>Fügt dem Bußgeld einen Zahlungseingang hinzu (alt: Sub-Tabelle in CInfoFrameStatusSub).
   */
  @PostMapping("/{id}/eingaenge")
  public ResponseEntity<BussgeldResponse> eingangHinzufuegen(
      @PathVariable Long id, @Valid @RequestBody EingangRequest r) {
    BussgeldResponse aktualisiert =
        bussgeldService.eingangHinzufuegen(id, r.datum(), r.betrag(), r.bemerkung());
    return ResponseEntity.status(HttpStatus.CREATED).body(aktualisiert);
  }

  @DeleteMapping("/{id}/eingaenge/{eingangId}")
  public BussgeldResponse eingangEntfernen(@PathVariable Long id, @PathVariable Long eingangId) {
    return bussgeldService.eingangEntfernen(id, eingangId);
  }
}
