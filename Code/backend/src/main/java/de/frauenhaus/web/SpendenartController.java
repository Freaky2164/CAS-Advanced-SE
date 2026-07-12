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
 * @author Nils
 *
 * REST-Endpunkte zur Pflege der Spendenarten (Stammdaten-CRUD).
 */
@RestController
@RequestMapping("/api/spendenarten")
public class SpendenartController {

    public record SpendenartRequest(@NotBlank String name, @NotBlank String spendentyp) { }
    public record SpendentypRequest(@NotBlank String spendentyp) { }

    private final SpendenartService spendenartService;

    public SpendenartController(SpendenartService spendenartService) {
        this.spendenartService = spendenartService;
    }

    @GetMapping
    public List<Spendenart> alle() {
        return spendenartService.alle();
    }

    @GetMapping("/{name}")
    public Spendenart finden(@PathVariable String name) {
        return spendenartService.finden(name);
    }

    @PostMapping
    public ResponseEntity<Spendenart> anlegen(@Valid @RequestBody SpendenartRequest request) {
        Spendenart angelegt = spendenartService.anlegen(request.name(), request.spendentyp());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    @PutMapping("/{name}")
    public Spendenart spendentypAendern(@PathVariable String name, @Valid @RequestBody SpendentypRequest request) {
        return spendenartService.spendentypAendern(name, request.spendentyp());
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        spendenartService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
