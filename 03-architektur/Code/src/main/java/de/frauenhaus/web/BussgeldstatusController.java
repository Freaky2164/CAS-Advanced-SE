package de.frauenhaus.web;

import de.frauenhaus.domain.Bussgeldstatus;
import de.frauenhaus.service.BussgeldstatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Endpunkte zur Pflege der zulässigen Bußgeld-Status (Stammdaten-Lookup).
 *
 * @author Nils
 */
@RestController
@RequestMapping("/api/bussgeldstatus")
public class BussgeldstatusController {

    /**
     * Datenobjekt der REST-Schnittstelle.
     */
    public record BussgeldstatusRequest(@NotBlank String name) { }

    private final BussgeldstatusService bussgeldstatusService;

    /**
     * Erzeugt den Controller mit den benötigten Services.
     */
    public BussgeldstatusController(BussgeldstatusService bussgeldstatusService) {
        this.bussgeldstatusService = bussgeldstatusService;
    }

    /**
     * Liefert die Einträge, optional gefiltert bzw. seitenweise.
     */
    @GetMapping
    public List<Bussgeldstatus> alle() {
        return bussgeldstatusService.alle();
    }

    /**
     * Legt einen neuen Eintrag an.
     */
    @PostMapping
    public ResponseEntity<Bussgeldstatus> anlegen(@Valid @RequestBody BussgeldstatusRequest request) {
        Bussgeldstatus angelegt = bussgeldstatusService.anlegen(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    /**
     * Löscht den Eintrag.
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        bussgeldstatusService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
