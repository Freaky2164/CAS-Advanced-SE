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
 * @author Nils
 *
 * REST-Endpunkte zur Pflege der zulässigen Bußgeld-Status (Stammdaten-Lookup).
 */
@RestController
@RequestMapping("/api/bussgeldstatus")
public class BussgeldstatusController {

    public record BussgeldstatusRequest(@NotBlank String name) { }

    private final BussgeldstatusService bussgeldstatusService;

    public BussgeldstatusController(BussgeldstatusService bussgeldstatusService) {
        this.bussgeldstatusService = bussgeldstatusService;
    }

    @GetMapping
    public List<Bussgeldstatus> alle() {
        return bussgeldstatusService.alle();
    }

    @PostMapping
    public ResponseEntity<Bussgeldstatus> anlegen(@Valid @RequestBody BussgeldstatusRequest request) {
        Bussgeldstatus angelegt = bussgeldstatusService.anlegen(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Void> loeschen(@PathVariable String name) {
        bussgeldstatusService.loeschen(name);
        return ResponseEntity.noContent().build();
    }
}
