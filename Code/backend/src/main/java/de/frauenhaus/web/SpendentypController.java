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
 * REST-Endpunkte zur Pflege der zulässigen Spendentypen (Stammdaten-Lookup).
 *
 * @author Ole
 */
@RestController
@RequestMapping("/api/spendentypen")
public class SpendentypController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record SpendentypRequest(@NotBlank String name) { }

    private final SpendentypService spendentypService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public SpendentypController(SpendentypService spendentypService) {
        this.spendentypService = spendentypService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public List<Spendentyp> alle() {
        return spendentypService.alle();
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<Spendentyp> anlegen(@Valid @RequestBody SpendentypRequest request) {
        Spendentyp angelegt = spendentypService.anlegen(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Löscht den Eintrag.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        spendentypService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
