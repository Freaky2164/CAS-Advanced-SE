package de.frauenhaus.web;

import de.frauenhaus.domain.Spendenart;
import de.frauenhaus.service.SpendenartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Endpunkte zur Pflege der Spendenarten (Stammdaten-CRUD).
 *
 * @author Robin
 */
@RestController
@RequestMapping("/api/spendenarten")
public class SpendenartController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record SpendenartRequest(@NotBlank String name, @NotBlank String spendentyp) { }
    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record SpendentypRequest(@NotBlank String spendentyp) { }

    private final SpendenartService spendenartService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public SpendenartController(SpendenartService spendenartService) {
        this.spendenartService = spendenartService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public List<Spendenart> alle() {
        return spendenartService.alle();
    }

    /**
     * Liefert einen Eintrag anhand seines Schlüssels.
     */
    @GetMapping("/{name}")
    public Spendenart finden(@PathVariable String name) {
        return spendenartService.finden(name);
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<Spendenart> anlegen(@Valid @RequestBody SpendenartRequest request) {
        Spendenart angelegt = spendenartService.anlegen(request.name(), request.spendentyp());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Ändert den Spendentyp einer Spendenart.
     */
    @PutMapping("/{name}")
    public Spendenart spendentypAendern(@PathVariable String name, @Valid @RequestBody SpendentypRequest request) {
        return spendenartService.spendentypAendern(name, request.spendentyp());
    }

    /**
     * Löscht den Eintrag.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        spendenartService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
