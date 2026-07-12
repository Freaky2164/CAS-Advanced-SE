package de.frauenhaus.web;

import de.frauenhaus.domain.Mitglied;
import de.frauenhaus.service.AuditService;
import de.frauenhaus.service.AuditService.VerlaufEintrag;
import de.frauenhaus.service.MitgliedService;
import de.frauenhaus.service.MitgliedService.MitgliedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Nils
 *
 * REST-Endpunkte zur Pflege der Mitglieder/Adressen (Stammdaten-CRUD),
 * inkl. Duplizieren (alt: CInfoFrameStatusCopy) und Verteiler-/Vereinszuordnung.
 */
@RestController
@RequestMapping("/api/mitglieder")
public class MitgliedController {

    /** Anfrage zum Anlegen/Ändern eines Mitglieds; {@code stichworte}/{@code vereine} sind optional. */
    public record MitgliedRequest(
            String anrede, String vorname, @NotBlank String name, String name2, String name3,
            String briefanrede, String strasse, String plz, String ort, String email,
            String tel1, String tel2, String fax, boolean foerderverein, boolean frauenhaus,
            String bemerkung, List<String> stichworte, List<String> vereine) {

        Mitglied toEntity() {
            Mitglied m = new Mitglied();
            m.setAnrede(anrede);
            m.setVorname(vorname);
            m.setName(name);
            m.setName2(name2);
            m.setName3(name3);
            m.setBriefanrede(briefanrede);
            m.setStrasse(strasse);
            m.setPlz(plz);
            m.setOrt(ort);
            m.setEmail(email);
            m.setTel1(tel1);
            m.setTel2(tel2);
            m.setFax(fax);
            m.setFoerderverein(foerderverein);
            m.setFrauenhaus(frauenhaus);
            m.setBemerkung(bemerkung);
            return m;
        }
    }

    private final MitgliedService mitgliedService;
    private final AuditService auditService;

    public MitgliedController(MitgliedService mitgliedService, AuditService auditService) {
        this.mitgliedService = mitgliedService;
        this.auditService = auditService;
    }

    @GetMapping
    public Page<MitgliedResponse> alle(Pageable pageable,
                                       @RequestParam(required = false) String suche) {
        return mitgliedService.alle(pageable, suche);
    }

    @GetMapping("/{id}")
    public MitgliedResponse finden(@PathVariable Long id) {
        return mitgliedService.finden(id);
    }

    @GetMapping("/{id}/verlauf")
    public List<VerlaufEintrag> verlauf(@PathVariable Long id) {
        return auditService.verlauf(Mitglied.class, id);
    }

    @PostMapping
    public ResponseEntity<MitgliedResponse> anlegen(@Valid @RequestBody MitgliedRequest request) {
        MitgliedResponse angelegt = mitgliedService.anlegen(request.toEntity(), request.stichworte(), request.vereine());
        return ResponseEntity.status(HttpStatus.CREATED).body(angelegt);
    }

    @PutMapping("/{id}")
    public MitgliedResponse aendern(@PathVariable Long id, @Valid @RequestBody MitgliedRequest request) {
        return mitgliedService.aendern(id, request.toEntity(), request.stichworte(), request.vereine());
    }

    /** Dupliziert ein Mitglied inkl. Stammdaten und Zuordnungen. */
    @PostMapping("/{id}/duplizieren")
    public ResponseEntity<MitgliedResponse> duplizieren(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mitgliedService.duplizieren(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> loeschen(@PathVariable Long id) {
        mitgliedService.loeschen(id);
        return ResponseEntity.noContent().build();
    }
}
