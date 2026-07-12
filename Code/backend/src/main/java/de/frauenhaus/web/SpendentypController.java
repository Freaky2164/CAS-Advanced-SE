package de.frauenhaus.web;

import de.frauenhaus.domain.Spendentyp;
import de.frauenhaus.service.SpendentypService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Nils
 *
 * REST-Endpunkte zur Pflege der zulässigen Spendentypen (Stammdaten-Lookup).
 */
@RestController
@RequestMapping("/api/spendentypen")
public class SpendentypController {

    public record SpendentypRequest(@NotBlank String name) { }

    private final SpendentypService spendentypService;

    public SpendentypController(SpendentypService spendentypService) {
        this.spendentypService = spendentypService;
    }

    @GetMapping
    public List<Spendentyp> alle() {
        return spendentypService.alle();
    }

    @PostMapping
    public ResponseEntity<Spendentyp> anlegen(@Valid @RequestBody SpendentypRequest request) {
        Spendentyp angelegt = spendentypService.anlegen(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        spendentypService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
