package de.frauenhaus.web;

import de.frauenhaus.service.StichwortService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nils
 *     <p>REST-Endpunkte für die Pflege von Verteiler-Stichworten (Zusammenstellen und
 *     Zusammenfassen bestehender Stichworte).
 */
@RestController
@RequestMapping("/api/stichworte")
public class StichwortController {

  /**
   * @author Nils
   *     <p>Anfrage zum Zusammenführen: {@code neu} ist das Ziel-Stichwort, {@code alte} die
   *     Quell-Stichworte.
   */
  public record ZusammenfuehrenRequest(@NotBlank String neu, @NotEmpty List<String> alte) {}

  private final StichwortService stichwortService;

  public StichwortController(StichwortService stichwortService) {
    this.stichwortService = stichwortService;
  }

  /**
   * @author Nils
   *     <p>Ordnet dem neuen Stichwort alle Mitglieder der alten Stichworte zu; die alten bleiben
   *     erhalten.
   */
  @PostMapping("/zusammenstellen")
  public Map<String, Integer> zusammenstellen(@RequestBody ZusammenfuehrenRequest request) {
    return Map.of("zugeordnet", stichwortService.zusammenstellen(request.neu(), request.alte()));
  }

  /**
   * @author Nils
   *     <p>Wie {@link #zusammenstellen}, löscht danach aber die alten Stichworte und ihre
   *     Zuordnungen.
   */
  @PostMapping("/zusammenfassen")
  public Map<String, Integer> zusammenfassen(@RequestBody ZusammenfuehrenRequest request) {
    return Map.of("zugeordnet", stichwortService.zusammenfassen(request.neu(), request.alte()));
  }
}
