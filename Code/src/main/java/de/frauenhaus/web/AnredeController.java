package de.frauenhaus.web;

import de.frauenhaus.domain.Anrede;
import de.frauenhaus.service.AnredeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Endpunkte zur Pflege der zulässigen Anreden (Stammdaten-Lookup).
 *
 * @author Nils
 */
@RestController
@RequestMapping("/api/anreden")
public class AnredeController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record AnredeRequest(@NotBlank String name) { }

    private final AnredeService anredeService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public AnredeController(AnredeService anredeService) {
        this.anredeService = anredeService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public List<Anrede> alle() {
        return anredeService.alle();
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<Anrede> anlegen(@Valid @RequestBody AnredeRequest request) {
        Anrede angelegt = anredeService.anlegen(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Löscht den Eintrag.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        anredeService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
