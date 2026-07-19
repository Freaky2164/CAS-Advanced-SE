package de.frauenhaus.web;

import de.frauenhaus.domain.Anrede;
import de.frauenhaus.service.AnredeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Nils
 *     <p>REST-Endpunkte zur Pflege der zulässigen Anreden (Stammdaten-Lookup).
 */
@RestController
@RequestMapping("/api/anreden")
public class AnredeController {

  public record AnredeRequest(@NotBlank String name) {}

  private final AnredeService anredeService;

  public AnredeController(AnredeService anredeService) {
    this.anredeService = anredeService;
  }

  @GetMapping
  public List<Anrede> alle() {
    return anredeService.alle();
  }

  @PostMapping
  public ResponseEntity<Anrede> anlegen(@Valid @RequestBody AnredeRequest request) {
    Anrede angelegt = anredeService.anlegen(request.name());
    return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
  }

  @DeleteMapping("/{name}")
  public ResponseEntity<Void> loeschen(@PathVariable String name) {
    anredeService.loeschen(name);
    return ResponseEntity.noContent().build();
  }
}
